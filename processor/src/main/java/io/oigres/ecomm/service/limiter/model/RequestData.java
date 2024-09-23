package io.oigres.ecomm.service.limiter.model;

import java.time.LocalDateTime;

import io.oigres.ecomm.service.limiter.RequestAudit;
import io.oigres.ecomm.service.limiter.ResponseAudit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class RequestData {

    private LocalDateTime requestArrived;
    private RequestAudit request;
    private LocalDateTime responseArrived;
    private ResponseAudit response;

}
