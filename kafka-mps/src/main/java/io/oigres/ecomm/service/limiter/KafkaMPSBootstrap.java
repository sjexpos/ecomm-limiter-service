package io.oigres.ecomm.service.limiter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@ComponentScan( basePackageClasses = { KafkaMPSBootstrap.class } )
@ConfigurationPropertiesScan
public class KafkaMPSBootstrap {
    
	public static void main(String[] args) {
		SpringApplication.run(KafkaMPSBootstrap.class, args);
	}

	@Bean
	public RestTemplate restTemplate(
			RestTemplateBuilder builder,
			@Value("${ecomm.service.limiter.processor.baseUri}") String baseUri
	) {
		return builder.rootUri(baseUri).build();
	}

}
