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

public class ResponseAuditTest {

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
        ResponseAudit response = ResponseAudit.builder()
                .id("abc123")
                .userId("123")
                .headers(Map.of("Content-Type", List.of("application/json")))
                .cookies(Map.of("data", List.of(ResponseAudit.HttpCookie.builder().name("key").value("qwerty").build() )))
                .status(201)
                .arrived(LocalDateTime.of(2024, 3, 10, 12, 15, 30, 0))
                .build();

        // when
        String json = mapper.writeValueAsString(response);
        // then
        Assertions.assertNotNull(json);

        // when
        ResponseAudit deserialized = mapper.readValue(json, ResponseAudit.class);
        // then
        Assertions.assertNotNull(deserialized);
        Assertions.assertEquals(response.getId(), deserialized.getId());
        Assertions.assertEquals(response.getUserId(), deserialized.getUserId());
        Assertions.assertEquals(response.getHeaders(), deserialized.getHeaders());
        Assertions.assertEquals(response.getStatus(), deserialized.getStatus());
        Assertions.assertEquals(response.getArrived(), deserialized.getArrived());

    }

}