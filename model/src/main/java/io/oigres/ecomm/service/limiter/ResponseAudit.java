package io.oigres.ecomm.service.limiter;

import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

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

}
