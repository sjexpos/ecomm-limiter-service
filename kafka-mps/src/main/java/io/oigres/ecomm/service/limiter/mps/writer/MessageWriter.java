package io.oigres.ecomm.service.limiter.mps.writer;

import io.oigres.ecomm.service.limiter.DLQMessage;
import io.oigres.ecomm.service.limiter.config.MessageWriterProperties;
import io.oigres.ecomm.service.limiter.mps.KafkaMessage;
import io.oigres.ecomm.service.limiter.mps.reader.MessageReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.Duration;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MessageWriter implements Runnable {
    private final MessageReader messageReader;
    private final RestApiCaller restApiCaller;
    private final KafkaTemplate<String, Object> messageKafkaTemplate;
    private final Duration pollTimeout;

    public MessageWriter(
            MessageReader messageReader,
            RestApiCaller restApiCaller,
            KafkaTemplate<String, Object> messageKafkaTemplate,
            MessageWriterProperties properties
    ) {
        this.messageReader = messageReader;
        this.restApiCaller = restApiCaller;
        this.messageKafkaTemplate = messageKafkaTemplate;
        this.pollTimeout = properties.getPollTimeout();
    }

    public void run() {
        while(true) {
            KafkaMessage msg = null;
            try {
                msg = this.messageReader.poll(this.pollTimeout);
            } catch (InterruptedException e) {
                break;
            }
            if (msg != null) {
                process(msg);
                this.messageReader.acknowledgeIfPossible(msg);
            }
        }
    }

    private void process(KafkaMessage message) {
        log.info("Processing message for key {}", message.getRecord().key() != null ? message.getRecord().key() : null);
        try {
            this.restApiCaller.call(message.getRecord().value());
        } catch (HttpServerErrorException | HttpClientErrorException e) {
            sendDLQ(message, e);
        } catch (Throwable t) {
            log.error("Unexpected error: ", t);
            sendDLQ(message, t);
        }
        message.setProcessed(true);
    }

    private void sendDLQ(KafkaMessage message, Throwable t) {
        log.info("Sending to DLQ message for key {}", message.getRecord().key() != null ? message.getRecord().key() : null);
        DLQMessage dlqMessage = DLQMessage.builder()
                .data(message.getRecord().value())
                .error(t.getMessage())
                .build();
        this.messageKafkaTemplate.sendDefault(message.getRecord().key(), dlqMessage);
    }

}
