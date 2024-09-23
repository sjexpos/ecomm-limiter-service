package io.oigres.ecomm.service.limiter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaTemplate<String, Object> messageKafkaTemplate(ProducerFactory<String, Object> messageProducerFactory) {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(messageProducerFactory);
        template.setObservationEnabled(true);
        return template;
    }

}
