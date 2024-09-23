package io.oigres.ecomm.service.limiter.mps.writer;

import io.github.resilience4j.retry.annotation.Retry;
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
        this.restTemplate.postForEntity("/api/v1/consume", payload, Void.class);
    }

}
