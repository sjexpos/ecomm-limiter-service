package io.oigres.ecomm.service.limiter.mps;

import io.oigres.ecomm.service.limiter.mps.reader.MessageReader;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

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
        log.trace("Consumed kafka message");
        try {
            messageReader.queue(record, ack);
        } catch (Throwable t) {
            log.error("Unexpected error: ", t);
        }
    }

}
