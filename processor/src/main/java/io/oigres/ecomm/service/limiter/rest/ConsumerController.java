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

package io.oigres.ecomm.service.limiter.rest;

import io.oigres.ecomm.service.limiter.RequestAudit;
import io.oigres.ecomm.service.limiter.ResponseAudit;
import io.oigres.ecomm.service.limiter.services.RequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/consume")
@Tag(name = "Consume", description = " ")
@Slf4j
@Validated
@RequiredArgsConstructor
public class ConsumerController {
  private final RequestService requestService;

  @Operation(
      summary = "Consume RequestAudit",
      description = "This endpoint is called to process a user request")
  @PostMapping(value = "/request", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<String> consumeRequest(@RequestBody @Valid RequestAudit requestAudit) {
    this.requestService.requestArrive(requestAudit);
    return ResponseEntity.ok("DONE");
  }

  @Operation(
      summary = "Consume ResponseAudit",
      description = "This endpoint is called to process a user response")
  @PostMapping(value = "/response", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<String> consumeResponse(@RequestBody @Valid ResponseAudit responseAudit) {
    this.requestService.responseArrive(responseAudit);
    return ResponseEntity.ok("DONE");
  }
}
