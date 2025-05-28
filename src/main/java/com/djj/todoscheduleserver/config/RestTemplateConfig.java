package com.djj.todoscheduleserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate配置类
 * 用于创建和配置RestTemplate Bean
 */
@Configuration
public class RestTemplateConfig {
    
    /**
     * 创建RestTemplate Bean用于HTTP请求
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
} 