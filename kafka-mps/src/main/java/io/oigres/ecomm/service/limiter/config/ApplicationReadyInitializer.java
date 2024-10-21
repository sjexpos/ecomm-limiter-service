package io.oigres.ecomm.service.limiter.config;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Order(0)
@Slf4j
public class ApplicationReadyInitializer implements ApplicationListener<ApplicationReadyEvent> {
    
	@Autowired
	private Environment environment;

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("");
        log.info("********************************************************************");
		log.info(" LIMITER SERVICE STARTED");
        log.info("********************************************************************");
		log.info("Default Charset: {}", Charset.defaultCharset());
		log.info("File Encoding:   {}", System.getProperty("file.encoding"));
		log.info("Server time:     {}", ZonedDateTime.now());
		MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
		log.info("----------------------- JVM memory ----------------------");
		log.info("Max Heap size:     {}", FileUtils.byteCountToDisplaySize(memoryBean.getHeapMemoryUsage().getMax()));
		log.info("Initial Heap size: {}", FileUtils.byteCountToDisplaySize(memoryBean.getHeapMemoryUsage().getInit()));
		log.info("Heap usage:        {}", FileUtils.byteCountToDisplaySize(memoryBean.getHeapMemoryUsage().getUsed()));
		log.info("--------------------------- Kafka -----------------------");
        log.info("Hosts:    {}", environment.getProperty("spring.kafka.bootstrap-servers"));
		log.info("Group-ID: {}", environment.getProperty("spring.kafka.consumer.group-id"));
		log.info("-------------------------- Topic ----------------------");
		log.info("Topic name: {}", environment.getProperty("ecomm.service.limiter.topics.incoming-request"));
		log.info("DLQ:        {}", environment.getProperty("ecomm.service.limiter.topics.request-dlq.name"));
		log.info("-------------------------- Reader ----------------------");
		log.info("Queue timeout: {}", environment.getProperty("ecomm.service.limiter.mps.reader.queue-timeout"));
		log.info("Purge time: {}", environment.getProperty("ecomm.service.limiter.mps.reader.purge-time"));
		log.info("-------------------------- Writers ----------------------");
		log.info("Writer threads: {}", environment.getProperty("ecomm.service.limiter.mps.writer.threads"));
		log.info("Poll timeout: {}", environment.getProperty("ecomm.service.limiter.mps.writer.poll-timeout"));
		log.info("-------------------------- Tracing ----------------------");
		log.info("URL: {}", environment.getProperty("ecomm.service.tracing.url"));
        log.info("********************************************************************");
        log.info("");
	}
}
