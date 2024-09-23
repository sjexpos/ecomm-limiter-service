package io.oigres.ecomm.service.limiter.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/api/v1")
public class RequestController {

    @GetMapping("/request/{user_id}")
    public String getRequestByUser(@PathVariable("user_id") String userId) {
        return new String();
    }

}
