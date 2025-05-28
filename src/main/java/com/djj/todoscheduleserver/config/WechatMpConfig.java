package com.djj.todoscheduleserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 微信公众号配置类
 * 用于从配置文件中读取微信相关配置参数
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wechat.mp")
public class WechatMpConfig {
    
    /**
     * 微信公众号AppID
     */
    private String appId;
    
    /**
     * 微信公众号Secret
     */
    private String secret;
    
    /**
     * 微信公众号Token
     */
    private String token;
    
    /**
     * 微信消息加密密钥
     */
    private String aesKey;
    
    /**
     * 模板消息ID配置
     */
    private Templates templates = new Templates();
    
    /**
     * 模板消息ID内部类
     */
    @Data
    public static class Templates {
        /**
         * 提醒消息模板ID
         */
        private String reminderTemplateId;
    }
} 