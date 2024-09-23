package io.oigres.ecomm.service.limiter;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class BlackedInfo {

    @JsonProperty("user_id")
    private String userId;
    private LocalDateTime from;
    private LocalDateTime to;

}
