package io.oigres.ecomm.service.limiter;

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
public class RequestAudit {

    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HttpCookie {
        private String name;
        private String value;
    }

    private String id;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("remote_addr")
    private String remoteAddr;
    private String method;
    private String path;
    private Map<String, List<String>> query;
    private Map<String, List<String>> headers;
    private Map<String, List<HttpCookie>> cookies;
    private String body;
    private LocalDateTime arrived;

}
