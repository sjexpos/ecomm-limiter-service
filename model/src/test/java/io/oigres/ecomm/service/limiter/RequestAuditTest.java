package io.oigres.ecomm.service.limiter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class RequestAuditTest {

    static private ObjectMapper mapper;

    @BeforeAll
    static void setup() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void test_serialize_deserialize() throws JsonProcessingException {

        // given
        RequestAudit request = RequestAudit.builder()
                .id("abc123")
                .userId("123")
                .remoteAddr("192.168.0.15")
                .method("GET")
                .path("/api/categories")
                .query(Map.of("limit", List.of("15")))
                .headers(Map.of("Content-Type", List.of("application/json")))
                .cookies(Map.of("data", List.of(RequestAudit.HttpCookie.builder().name("key").value("qwerty").build() )))
                .body("request body")
                .arrived(LocalDateTime.of(2024, 3, 10, 12, 15, 30, 0))
                .build();

        // when
        String json = mapper.writeValueAsString(request);
        // then
        Assertions.assertNotNull(json);

        // when
        RequestAudit deserialized = mapper.readValue(json, RequestAudit.class);
        // then
        Assertions.assertNotNull(deserialized);
        Assertions.assertEquals(request.getId(), deserialized.getId());
        Assertions.assertEquals(request.getUserId(), deserialized.getUserId());
        Assertions.assertEquals(request.getRemoteAddr(), deserialized.getRemoteAddr());
        Assertions.assertEquals(request.getMethod(), deserialized.getMethod());
        Assertions.assertEquals(request.getPath(), deserialized.getPath());
        Assertions.assertEquals(request.getQuery(), deserialized.getQuery());
        Assertions.assertEquals(request.getHeaders(), deserialized.getHeaders());
        Assertions.assertEquals(request.getBody(), deserialized.getBody());
        Assertions.assertEquals(request.getArrived(), deserialized.getArrived());
    }

}