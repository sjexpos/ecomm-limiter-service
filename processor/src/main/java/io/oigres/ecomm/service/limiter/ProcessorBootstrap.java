package io.oigres.ecomm.service.limiter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan( basePackageClasses = { ProcessorBootstrap.class } )
@ConfigurationPropertiesScan
public class ProcessorBootstrap {
    
	public static void main(String[] args) {
		SpringApplication.run(ProcessorBootstrap.class, args);
	}

}
