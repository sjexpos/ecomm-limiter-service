package io.oigres.ecomm.service.limiter.mps.writer;

import io.oigres.ecomm.service.limiter.DLQMessage;
import io.oigres.ecomm.service.limiter.mps.KafkaMessage;
import io.oigres.ecomm.service.limiter.mps.reader.MessageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class MessageWriter implements Runnable {
    private final MessageReader messageReader;
    private final RestApiCaller restApiCaller;
    private final KafkaTemplate<String, Object> messageKafkaTemplate;

    public void run() {
        while(true) {
            KafkaMessage msg = null;
            try {
                msg = this.messageReader.poll();
            } catch (InterruptedException e) {
                break;
            }
            process(msg);
            this.messageReader.acknowledgeIfPossible(msg);
        }
    }

    private void process(KafkaMessage message) {
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

    private void sendDLQ(KafkaMessage msg, Throwable t) {
        DLQMessage dlqMessage = DLQMessage.builder()
                .data(msg.getRecord().value())
                .error(t.getMessage())
                .build();
        this.messageKafkaTemplate.sendDefault(dlqMessage);
    }

}
