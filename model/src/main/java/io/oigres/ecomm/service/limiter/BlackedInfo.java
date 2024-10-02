package io.oigres.ecomm.service.limiter;

import java.time.LocalDateTime;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BlackedInfo {

    @JsonProperty("user_id")
    private String userId;
    private LocalDateTime from;
    private LocalDateTime to;

    public boolean isIncluded(LocalDateTime time) {
        return !time.isBefore(getFrom()) && !time.isAfter(getTo());
    }

}
