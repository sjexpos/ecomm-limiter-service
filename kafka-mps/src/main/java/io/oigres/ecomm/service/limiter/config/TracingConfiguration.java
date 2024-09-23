package io.oigres.ecomm.service.limiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Configuration
public class TracingConfiguration {

    @Data
    @ConfigurationProperties(prefix = "ecomm.service.tracing")
    public static class TracingProperties {
        @NotNull
        @NotBlank
        private String url;
    }

    @Bean
    public OtlpHttpSpanExporter otlpHttpSpanExporter(TracingProperties properties) {
        return OtlpHttpSpanExporter.builder()
            .setEndpoint(properties.getUrl())
            .build();
    }

}
