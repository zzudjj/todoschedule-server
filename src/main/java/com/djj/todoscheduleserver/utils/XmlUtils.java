package com.djj.todoscheduleserver.utils;

import com.djj.todoscheduleserver.pojo.wechat.WxMpXmlMessage;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.StringReader;

/**
 * XML处理工具类
 * 用于处理微信消息的XML解析和生成
 */
@Slf4j
public class XmlUtils {

    /**
     * 从XML字符串解析微信消息
     * 
     * @param xml XML字符串
     * @return 微信消息对象
     */
    public static WxMpXmlMessage parseXmlToMessage(String xml) {
        try {
            SAXReader saxReader = new SAXReader();
            Document document = saxReader.read(new StringReader(xml));
            Element rootElement = document.getRootElement();
            
            WxMpXmlMessage message = new WxMpXmlMessage();
            
            message.setToUser(getElementText(rootElement, "ToUserName"));
            message.setFromUser(getElementText(rootElement, "FromUserName"));
            String createTimeStr = getElementText(rootElement, "CreateTime");
            message.setCreateTime(createTimeStr != null ? Long.parseLong(createTimeStr) : 0);
            message.setMsgType(getElementText(rootElement, "MsgType"));
            message.setContent(getElementText(rootElement, "Content"));
            String msgIdStr = getElementText(rootElement, "MsgId");
            message.setMsgId(msgIdStr != null ? Long.parseLong(msgIdStr) : 0);
            message.setEvent(getElementText(rootElement, "Event"));
            message.setEventKey(getElementText(rootElement, "EventKey"));
            
            // 处理媒体类型消息特有的属性
            message.setMediaId(getElementText(rootElement, "MediaId"));
            message.setPicUrl(getElementText(rootElement, "PicUrl"));
            
            return message;
        } catch (DocumentException e) {
            log.error("解析XML失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从Element中获取子元素文本
     * 
     * @param root 根元素
     * @param elementName 子元素名称
     * @return 子元素文本
     */
    public static String getElementText(Element root, String elementName) {
        Element element = root.element(elementName);
        return element != null ? element.getText() : null;
    }
} 