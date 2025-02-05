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

package io.oigres.ecomm.service.limiter.mps.writer;

import io.github.resilience4j.retry.annotation.Retry;
import io.oigres.ecomm.service.limiter.RequestAudit;
import io.oigres.ecomm.service.limiter.ResponseAudit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class RestApiCaller {
  private final RestTemplate restTemplate;

  @Retry(name = "remote-consumer")
  public void call(Object payload) throws HttpClientErrorException, HttpServerErrorException {
    if (payload instanceof RequestAudit) {
      this.restTemplate.postForEntity("/api/v1/consume/request", payload, Void.class);
    } else if (payload instanceof ResponseAudit) {
      this.restTemplate.postForEntity("/api/v1/consume/response", payload, Void.class);
    } else {
      throw new IllegalArgumentException(
          payload != null
              ? String.format("Payload type '%s' is not supported", payload.getClass().getName())
              : "Argument is null");
    }
  }
}
