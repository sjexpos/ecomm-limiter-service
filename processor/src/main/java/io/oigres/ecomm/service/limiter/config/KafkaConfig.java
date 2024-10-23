package io.oigres.ecomm.service.limiter.config;

import io.oigres.ecomm.service.limiter.BlackedInfo;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

//    @Bean
//    public KafkaAdmin admin(@Value("${spring.kafka.bootstrap-servers}") String kafkaBrokers) {
//        Map<String, Object> configs = new HashMap<>();
//        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
//        return new KafkaAdmin(configs);
//    }

    @Bean
    public KafkaTemplate<String, BlackedInfo> messageKafkaTemplate(
            ProducerFactory<String, BlackedInfo> messageProducerFactory,
//            KafkaAdmin kafkaAdmin,
            BlacklistedUserTopicProperties topicConfig
    ) {
//        kafkaAdmin.createOrModifyTopics(new NewTopic(topicConfig.getName(), topicConfig.getPartitions(), topicConfig.getReplicationFactor()));
        KafkaTemplate<String, BlackedInfo> template = new KafkaTemplate<>(messageProducerFactory);
        template.setDefaultTopic(topicConfig.getName());
        template.setObservationEnabled(true);
        return template;
    }

}
