package io.oigres.ecomm.service.limiter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DLQMessage {
    private Object data;
    private String error;
}
