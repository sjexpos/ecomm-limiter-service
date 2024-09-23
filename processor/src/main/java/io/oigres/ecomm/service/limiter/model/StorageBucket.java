package io.oigres.ecomm.service.limiter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

@Builder
@Getter
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class StorageBucket {
    private final String userId;
    private final LocalDateTime minute;
    private final List<RequestData> requests = new LinkedList<>();
}
