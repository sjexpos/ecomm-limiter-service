/**********
 This project is free software; you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the
 Free Software Foundation; either version 3.0 of the License, or (at your
 option) any later version. (See <https://www.gnu.org/licenses/gpl-3.0.html>.)

 This project is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 more details.

 You should have received a copy of the GNU General Public License
 along with this project; if not, write to the Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 **********/
// Copyright (c) 2024-2025 Sergio Exposito.  All rights reserved.              

package io.oigres.ecomm.service.limiter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ResponseAuditTest {

  private static ObjectMapper mapper;

  @BeforeAll
  static void setup() {
    mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @Test
  void test_serialize_deserialize() throws JsonProcessingException {
    // given
    ResponseAudit response =
        ResponseAudit.builder()
            .id("abc123")
            .userId("123")
            .headers(Map.of("Content-Type", List.of("application/json")))
            .cookies(
                Map.of(
                    "data",
                    List.of(
                        ResponseAudit.HttpCookie.builder().name("key").value("qwerty").build())))
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
