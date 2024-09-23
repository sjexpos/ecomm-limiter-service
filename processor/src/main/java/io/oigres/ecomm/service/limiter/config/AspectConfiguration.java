package io.oigres.ecomm.service.limiter.config;

import io.oigres.ecomm.cache.CacheLockAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AspectConfiguration {

    @Bean
    public CacheLockAspect cacheLockAspect() {
        return new CacheLockAspect();
    }

}
