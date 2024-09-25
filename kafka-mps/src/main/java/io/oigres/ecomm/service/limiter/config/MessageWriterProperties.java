package io.oigres.ecomm.service.limiter.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "ecomm.service.limiter.mps.writer")
public class MessageWriterProperties {
    @NotNull
    @NotBlank
    private Integer threads;
    @NotNull
    @NotBlank
    private Duration pollTimeout;

}
