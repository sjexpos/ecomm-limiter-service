package io.oigres.ecomm.service.limiter;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ResponseAudit {

    @Builder
    @Getter
        public static class HttpCookie {
        private String name;
        private String value;
    }

    private String id;
    @JsonProperty("user_id")
    private String userId;
    private Map<String, List<String>> headers;
    private Map<String, List<HttpCookie>> cookies;
    private int status;
    private LocalDateTime arrived;

}
