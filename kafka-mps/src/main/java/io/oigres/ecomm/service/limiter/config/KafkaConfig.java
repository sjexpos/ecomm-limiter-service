package io.oigres.ecomm.service.limiter.config;

import io.oigres.ecomm.service.limiter.mps.writer.MessageWriter;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.tomcat.util.threads.VirtualThreadExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(1);
        factory.getContainerProperties().setObservationEnabled(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

//    @Bean
//    public KafkaAdmin admin(@Value("${spring.kafka.bootstrap-servers}") String kafkaBrokers) {
//        Map<String, Object> configs = new HashMap<>();
//        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
//        return new KafkaAdmin(configs);
//    }

    @Bean
    public KafkaTemplate<String, Object> messageKafkaTemplate(
            ProducerFactory<String, Object> messageProducerFactory,
//            KafkaAdmin kafkaAdmin,
            RequestDLQTopicProperties topicConfig
    ) {
//        kafkaAdmin.createOrModifyTopics(new NewTopic(topicConfig.getName(), topicConfig.getPartitions(), topicConfig.getReplicationFactor()));
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(messageProducerFactory);
        template.setDefaultTopic(topicConfig.getName());
        template.setObservationEnabled(true);
        return template;
    }

    @Bean
    public List<MessageWriter> messageWriters(
            ConfigurableBeanFactory beanFactory,
            MessageWriterProperties properties
    ) {
        List<MessageWriter> messageWriters = new ArrayList<>(properties.getThreads());
        IntStream.range(1, properties.getThreads())
                .forEach(i -> {
                    messageWriters.add( beanFactory.getBean(MessageWriter.class) );
                });
        VirtualThreadExecutor executor = new VirtualThreadExecutor("writer");
        messageWriters.forEach(executor::execute);
        return messageWriters;
    }

}
