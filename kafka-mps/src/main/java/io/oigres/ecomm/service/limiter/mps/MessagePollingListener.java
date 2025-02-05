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

import io.oigres.ecomm.service.limiter.mps.reader.MessageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * https://medium.com/walmartglobaltech/reliably-processing-trillions-of-kafka-messages-per-day-23494f553ef9
 *
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MessagePollingListener {
  private final MessageReader messageReader;

  @KafkaListener(topics = "${ecomm.service.limiter.topics.incoming-request}")
  public void consumeMessage(ConsumerRecord<String, Object> record, Acknowledgment ack) {
    if (record != null) {
      log.info(
          "Consumed kafka message from '{}'/'{}' in offset '{}' with key '{}'  ",
          record.topic(),
          record.partition(),
          record.offset(),
          record.key());
    }
    try {
      messageReader.queue(record, ack);
    } catch (Throwable t) {
      log.error("Unexpected error: ", t);
    }
  }
}
