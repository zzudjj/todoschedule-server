package com.djj.todoscheduleserver.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI配置类
 * 用于配置Swagger UI文档
 */
@Configuration
public class OpenApiConfig {

    @Value("${wechat.server-url}")
    private String serverUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("微信公众号任务提醒系统API")
                        .description("基于Spring Boot的微信公众号任务提醒系统，用于在任务开始前通过微信公众号发送提醒消息")
                        .version("1.0")
                        .contact(new Contact()
                                .name("开发团队")
                                .email("contact@example.com")
                                .url("https://example.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url(serverUrl)
                                .description("微信公众号服务器"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("本地开发服务器")
                ));
    }
} 