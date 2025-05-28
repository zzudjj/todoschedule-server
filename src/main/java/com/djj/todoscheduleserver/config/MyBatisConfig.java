package com.djj.todoscheduleserver.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.djj.todoscheduleserver.mapper")
public class MyBatisConfig {
    // @MapperScan注解将扫描mapper接口包
    // 并自动将其注册为Spring bean
} 