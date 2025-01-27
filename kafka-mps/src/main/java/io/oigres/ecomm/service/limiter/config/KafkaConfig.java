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

package io.oigres.ecomm.service.limiter.config;

import io.oigres.ecomm.service.limiter.mps.writer.MessageWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.tomcat.util.threads.VirtualThreadExecutor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;

@Configuration
@EnableKafka
public class KafkaConfig {

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
      ConsumerFactory<String, String> consumerFactory) {
    ConcurrentKafkaListenerContainerFactory<String, String> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
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
      RequestDLQTopicProperties topicConfig) {
    // This topic creation from code requires more permission on Kafka, so it is commented
    //        kafkaAdmin.createOrModifyTopics(new NewTopic(topicConfig.getName(),
    // topicConfig.getPartitions(), topicConfig.getReplicationFactor()));
    KafkaTemplate<String, Object> template = new KafkaTemplate<>(messageProducerFactory);
    template.setDefaultTopic(topicConfig.getName());
    template.setObservationEnabled(true);
    return template;
  }

  @Bean
  public List<MessageWriter> messageWriters(
      ConfigurableBeanFactory beanFactory, MessageWriterProperties properties) {
    List<MessageWriter> messageWriters = new ArrayList<>(properties.getThreads());
    IntStream.range(1, properties.getThreads())
        .forEach(
            i -> {
              messageWriters.add(beanFactory.getBean(MessageWriter.class));
            });
    VirtualThreadExecutor executor = new VirtualThreadExecutor("writer");
    messageWriters.forEach(executor::execute);
    return messageWriters;
  }
}
