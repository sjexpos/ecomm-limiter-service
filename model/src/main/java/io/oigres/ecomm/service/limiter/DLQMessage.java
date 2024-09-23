package io.oigres.ecomm.service.limiter;

import lombok.Builder;

@Builder
public class DLQMessage {
    private final Object data;
    private final String error;
}
