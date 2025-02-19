/**********
 This project is free software; you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the
 Free Software Foundation; either version 3.0 of the License, or (at your
 option) any later version. (See <https://www.gnu.org/licenses/gpl-3.0.html>.)

 This project is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 more details.

 You should have received a copy of the GNU General Public License
 along with this project; if not, write to the Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 **********/
// Copyright (c) 2024-2025 Sergio Exposito.  All rights reserved.              

package io.oigres.ecomm.service.limiter.mps.reader;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import io.oigres.ecomm.service.limiter.config.MessageReaderProperties;
import io.oigres.ecomm.service.limiter.mps.KafkaMessage;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.core.env.Environment;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
// @Observed
public class MessageReader {
  private final PendingQueue<KafkaMessage> pendingQueue;
  private final PendingQueue<KafkaMessage>.OrderIterator orderIterator;
  private final ConcurrentHashMap<String, Object> inflightSet;
  private final Duration offerTimeout;
  private final Duration maxPurgeTime;

  public MessageReader(
      MessageReaderProperties configuration, MeterRegistry registry, Environment env) {
    this.pendingQueue = new PendingQueue<KafkaMessage>(configuration.getQueueSize());
    this.orderIterator =
        (PendingQueue<KafkaMessage>.OrderIterator) this.pendingQueue.orderIterator();
    this.inflightSet = new ConcurrentHashMap<>();
    this.offerTimeout = configuration.getQueueTimeout();
    this.maxPurgeTime = configuration.getPurgeTime();
    Gauge.builder("mps.pending", this.pendingQueue, PendingQueue::size)
        .description("Kafka messages to be processed")
        .tag("region", "us-east")
        .baseUnit("messages")
        .register(registry);
  }

  @Observed(name = "mps.messages.queue")
  public Object queue(ConsumerRecord<String, Object> record, Acknowledgment ack)
      throws InterruptedException {
    boolean added = false;
    while (!added) {
      added = this.pendingQueue.offer(new KafkaMessage(record, ack), this.offerTimeout);
      if (!added && this.pendingQueue.remainingCapacity() == 0) {
        this.pendingQueue.purge(KafkaMessage::isAcknowledged, this.maxPurgeTime);
      }
    }
    return null;
  }

  @Observed(name = "mps.messages.poll")
  public KafkaMessage poll(Duration timeout) throws InterruptedException {
    KafkaMessage msg = this.orderIterator.next(timeout);
    if (msg != null) {
      msg.getContext().makeCurrent(); // Opentelemetry propagation
      if (msg.getRecord() != null && msg.getRecord().key() != null) {
        this.inflightSet.put(msg.getRecord().key(), msg);
      } else {
        log.warn("Trying to process e message without record or key!");
      }
    }
    return msg;
  }

  @Observed(name = "mps.messages.acknowledge")
  public Object acknowledgeIfPossible(KafkaMessage message) {
    final AtomicBoolean wasPrevAcknowledged = new AtomicBoolean(true);
    this.pendingQueue.forEach(
        m -> {
          if (m.getRecord().partition() != message.getRecord().partition()) {
            return;
          }
          if (wasPrevAcknowledged.get()) {
            m.acknowledge();
          }
          wasPrevAcknowledged.set(m.isAcknowledged());
        },
        m -> (m.getRecord().partition() == message.getRecord().partition()) && !m.isAcknowledged());
    return null;
  }
}
