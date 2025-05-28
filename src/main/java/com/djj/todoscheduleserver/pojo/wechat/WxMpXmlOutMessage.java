package com.djj.todoscheduleserver.pojo.wechat;

import com.djj.todoscheduleserver.utils.Constants;
import lombok.Data;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

/**
 * 微信XML输出消息
 * 用于构建回复给微信服务器的XML消息
 */
@Data
public class WxMpXmlOutMessage {
    private String toUser;
    private String fromUser;
    private Long createTime;
    private String msgType;
    private String content;
    private String mediaId;  // 用于图片等媒体类型消息
    
    /**
     * 创建文本类型消息
     * 
     * @return 微信XML输出消息
     */
    public static WxMpXmlOutMessage TEXT() {
        WxMpXmlOutMessage message = new WxMpXmlOutMessage();
        message.setMsgType(Constants.MessageType.TEXT);
        message.setCreateTime(System.currentTimeMillis() / 1000);
        return message;
    }
    
    /**
     * 创建图片类型消息
     * 
     * @return 微信XML输出消息
     */
    public static WxMpXmlOutMessage IMAGE() {
        WxMpXmlOutMessage message = new WxMpXmlOutMessage();
        message.setMsgType(Constants.MessageType.IMAGE);
        message.setCreateTime(System.currentTimeMillis() / 1000);
        return message;
    }
    
    /**
     * 将消息转换为XML字符串
     * 
     * @return XML字符串
     */
    public String toXml() {
        // 使用DOM4J创建XML文档
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement("xml");
        
        // 添加基础元素
        root.addElement("ToUserName").addCDATA(this.toUser);
        root.addElement("FromUserName").addCDATA(this.fromUser);
        root.addElement("CreateTime").setText(String.valueOf(this.createTime));
        root.addElement("MsgType").addCDATA(this.msgType);
        
        // 根据消息类型添加不同的内容
        if (Constants.MessageType.TEXT.equals(this.msgType)) {
            root.addElement("Content").addCDATA(this.content);
        } else if (Constants.MessageType.IMAGE.equals(this.msgType)) {
            Element image = root.addElement("Image");
            image.addElement("MediaId").addCDATA(this.mediaId);
        }
        
        // 返回XML字符串
        return document.asXML();
    }
} 