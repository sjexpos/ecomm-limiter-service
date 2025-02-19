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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ResponseAudit {

  @Builder
  @Getter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class HttpCookie {
    private String name;
    private String value;
    private Duration maxAge;
    private String domain;
    private String path;
    private boolean secure;
    private boolean httpOnly;
    private String sameSite;
  }

  private String id;

  @JsonProperty("user_id")
  private String userId;

  private Map<String, List<String>> headers;
  private Map<String, List<HttpCookie>> cookies;
  private int status;
  private LocalDateTime arrived;

  public Map<String, List<String>> getHeaders() {
    return Objects.isNull(headers)
        ? null
        : this.headers.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> List.copyOf(e.getValue())));
  }

  public Map<String, List<HttpCookie>> getCookies() {
    return Objects.isNull(cookies)
        ? null
        : this.cookies.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> List.copyOf(e.getValue())));
  }
}
