package io.oigres.ecomm.service.limiter.config;

import io.oigres.ecomm.service.limiter.mps.writer.MessageWriter;
import org.apache.tomcat.util.threads.VirtualThreadExecutor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(1);
        return factory;
    }

    @Bean
    public KafkaTemplate<String, Object> messageKafkaTemplate(ProducerFactory<String, Object> messageProducerFactory) {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(messageProducerFactory);
        template.setDefaultTopic("DLQ");
        template.setObservationEnabled(true);
        return template;
    }

    @Bean
    public List<MessageWriter> messageWriters(ConfigurableBeanFactory beanFactory) {
        List<MessageWriter> messageWriters = new ArrayList<>(50);
        IntStream.range(1, 50)
                .forEach(i -> {
                    messageWriters.add( beanFactory.getBean(MessageWriter.class) );
                });
        VirtualThreadExecutor executor = new VirtualThreadExecutor("writer");
        messageWriters.forEach(executor::execute);
        return messageWriters;
    }

}
