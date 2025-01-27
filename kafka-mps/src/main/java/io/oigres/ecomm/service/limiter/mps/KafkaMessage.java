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

package io.oigres.ecomm.service.limiter.mps;

import io.opentelemetry.context.Context;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;

@RequiredArgsConstructor
public class KafkaMessage {

  @Getter private final ConsumerRecord<String, Object> record;
  private final Acknowledgment ack;
  private ReentrantLock lock = new ReentrantLock();
  private AtomicBoolean processed = new AtomicBoolean(false);
  private AtomicBoolean acknowledged = new AtomicBoolean(false);
  @Getter private Context context = Context.current();

  public void acknowledge() {
    if (!this.processed.get()) {
      return;
    }
    lock.lock();
    try {
      if (!this.acknowledged.get()) {
        ack.acknowledge();
        this.acknowledged.set(true);
      }
    } finally {
      lock.unlock();
    }
  }

  public void setProcessed(boolean value) {
    this.processed.set(value);
  }

  public boolean isProcessed() {
    return this.processed.get();
  }

  public boolean isAcknowledged() {
    return this.acknowledged.get();
  }
}
