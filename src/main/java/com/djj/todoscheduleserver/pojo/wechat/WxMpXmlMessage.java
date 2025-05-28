package com.djj.todoscheduleserver.pojo.wechat;

import lombok.Data;

/**
 * 微信XML消息
 * 用于解析接收自微信服务器的XML消息
 */
@Data
public class WxMpXmlMessage {
    private String toUser;
    private String fromUser;
    private Long createTime;
    private String msgType;
    private String content;
    private Long msgId;
    private String event;
    private String eventKey;
    private String mediaId;     // 图片、语音等媒体消息的MediaID
    private String picUrl;      // 图片链接
} 