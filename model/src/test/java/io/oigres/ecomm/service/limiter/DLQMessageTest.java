package io.oigres.ecomm.service.limiter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DLQMessageTest {
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
        DLQMessage msg = DLQMessage.builder()
                .data("data test")
                .error("error test")
                .build();
        // when
        String json = mapper.writeValueAsString(msg);
        // then
        Assertions.assertNotNull(json);

        // when
        DLQMessage deserialized = mapper.readValue(json, DLQMessage.class);
        // then
        Assertions.assertNotNull(deserialized);
        Assertions.assertEquals(msg.getData(), deserialized.getData());
        Assertions.assertEquals(msg.getError(), deserialized.getError());
    }

}