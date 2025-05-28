package com.djj.todoscheduleserver.service;

import com.djj.todoscheduleserver.pojo.wechat.WxMpXmlMessage;
import com.djj.todoscheduleserver.pojo.wechat.WxMpXmlOutMessage;

public interface WechatService {
    
    /**
     * 处理接收到的微信消息
     * 
     * @param inMessage 接收到的消息
     * @return 要发送回去的响应消息
     */
    WxMpXmlOutMessage processMessage(WxMpXmlMessage inMessage);
    
    /**
     * 构建文本回复消息
     * @param toUser 接收方OpenID
     * @param fromUser 发送方OpenID (公众号原始ID)
     * @param content 回复内容
     * @return XML格式的文本回复消息
     */
    String buildTextResponse(String toUser, String fromUser, String content);
    
    /**
     * 向用户发送提醒消息
     * 
     * @param openId 用户的微信openId
     * @param title 日程的标题
     * @param startTime 日程的开始时间
     * @param endTime 日程的结束时间
     * @param location 地点（如果有）
     * @return 发送成功返回true
     */
    boolean sendReminderMessage(String openId, String title, long startTime, long endTime, String location);
    
    /**
     * 创建微信公众号自定义菜单
     * 
     * @return 创建成功返回true
     */
    boolean createCustomMenu();
} 