package io.oigres.ecomm.service.limiter.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ecomm.service.limiter.topics.request-dlq")
public class RequestDLQTopicProperties {
    @NotNull
    @NotBlank
    private String name;
    private int partitions;
    private short replicationFactor;
}
