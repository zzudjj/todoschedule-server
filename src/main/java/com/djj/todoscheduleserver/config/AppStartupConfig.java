package com.djj.todoscheduleserver.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动配置
 * 应用启动后显示访问URL
 */
@Slf4j
@Component
public class AppStartupConfig implements CommandLineRunner {

    @Value("${server.port:8080}")
    private int serverPort;

    @Override
    public void run(String... args) {
        log.info("======================================================");
        log.info("任务提醒微信公众号服务已启动！");
        log.info("本地访问: http://localhost:{}", serverPort);
        log.info("请确保已正确配置公众号相关参数和数据库连接");
        log.info("======================================================");
    }
} 