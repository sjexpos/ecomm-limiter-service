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
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class BlackedInfoTest {
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
    LocalDateTime from = LocalDateTime.of(2024, 3, 10, 12, 15, 30, 0);
    LocalDateTime to = LocalDateTime.of(2024, 3, 10, 13, 15, 30, 0);
    BlackedInfo blackedInfo = BlackedInfo.builder().userId("123").from(from).to(to).build();

    // when
    String json = mapper.writeValueAsString(blackedInfo);
    // then
    Assertions.assertNotNull(json);

    // when
    BlackedInfo deserialized = mapper.readValue(json, BlackedInfo.class);
    // then
    Assertions.assertNotNull(deserialized);
    Assertions.assertEquals(blackedInfo.getUserId(), deserialized.getUserId());
    Assertions.assertEquals(from, deserialized.getFrom());
    Assertions.assertEquals(to, deserialized.getTo());
  }

  private static Stream<Arguments> provideParameters() {
    return Stream.of(
        Arguments.of(LocalDateTime.of(2024, 3, 10, 12, 45, 30, 0), true),
        Arguments.of(LocalDateTime.of(2024, 3, 10, 12, 15, 30, 0), true),
        Arguments.of(LocalDateTime.of(2024, 3, 10, 13, 15, 30, 0), true),
        Arguments.of(LocalDateTime.of(2024, 3, 10, 12, 15, 29, 0), false),
        Arguments.of(LocalDateTime.of(2024, 3, 10, 13, 15, 31, 0), false),
        Arguments.of(LocalDateTime.of(2024, 3, 10, 12, 10, 29, 0), false),
        Arguments.of(LocalDateTime.of(2024, 3, 10, 13, 25, 31, 0), false));
  }

  @ParameterizedTest
  @MethodSource("provideParameters")
  void test_is_blocked(LocalDateTime test, boolean result) {
    // given
    LocalDateTime from = LocalDateTime.of(2024, 3, 10, 12, 15, 30, 0);
    LocalDateTime to = LocalDateTime.of(2024, 3, 10, 13, 15, 30, 0);
    BlackedInfo blackedInfo = BlackedInfo.builder().userId("123").from(from).to(to).build();

    // when
    boolean status = blackedInfo.isIncluded(test);

    // then
    Assertions.assertEquals(result, status);
  }
}
