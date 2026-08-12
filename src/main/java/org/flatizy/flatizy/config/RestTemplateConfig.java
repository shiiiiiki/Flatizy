package org.flatizy.flatizy.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory simpleFactory = new SimpleClientHttpRequestFactory();
        simpleFactory.setConnectTimeout(5000);
        simpleFactory.setReadTimeout(10000);

        return builder
                .requestFactory(() -> simpleFactory)
                .build();
    }
}

