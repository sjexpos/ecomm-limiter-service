package io.oigres.ecomm.service.limiter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@ComponentScan( basePackageClasses = { Bootstrap.class } )
@EnableKafka
public class Bootstrap {
    
	public static void main(String[] args) {
		SpringApplication.run(Bootstrap.class, args);
	}

}
