package io.oigres.ecomm.service.limiter.config;

import io.oigres.ecomm.service.limiter.BlackedInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaTemplate<String, BlackedInfo> messageKafkaTemplate(
            ProducerFactory<String, BlackedInfo> messageProducerFactory,
            @Value("${ecomm.service.limiter.topics.blacklisted-users}") String blacklistedUserTopicName
    ) {
        KafkaTemplate<String, BlackedInfo> template = new KafkaTemplate<>(messageProducerFactory);
        template.setDefaultTopic(blacklistedUserTopicName);
        template.setObservationEnabled(true);
        return template;
    }

}
