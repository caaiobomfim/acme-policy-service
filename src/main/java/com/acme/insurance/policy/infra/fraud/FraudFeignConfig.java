package com.acme.insurance.policy.infra.fraud;

import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;

public class FraudFeignConfig {

    @Bean
    public RequestInterceptor headers() {
        return tpl -> {
            tpl.header("Accept", MediaType.APPLICATION_JSON_VALUE);
        };
    }

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
