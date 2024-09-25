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
            throw new IllegalArgumentException(payload != null ? String.format("Payload type '%s' is not supported", payload.getClass().getName()) : "Argument is null");
        }
    }

}
