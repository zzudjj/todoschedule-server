package com.djj.todoscheduleserver.service.impl;

import com.djj.todoscheduleserver.config.WechatMpConfig;
import com.djj.todoscheduleserver.mapper.UserMapper;
import com.djj.todoscheduleserver.pojo.User;
import com.djj.todoscheduleserver.pojo.wechat.WxMpXmlMessage;
import com.djj.todoscheduleserver.pojo.wechat.WxMpXmlOutMessage;
import com.djj.todoscheduleserver.service.WechatService;
import com.djj.todoscheduleserver.utils.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 微信服务实现类
 * 处理微信消息和发送模板消息
 */
@Slf4j
@Service
public class WechatServiceImpl implements WechatService {

    @Autowired
    private WechatMpConfig wechatMpConfig;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    private static final String API_GET_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";
    private static final String API_SEND_TEMPLATE_URL = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=%s";
    private static final String API_CREATE_MENU_URL = "https://api.weixin.qq.com/cgi-bin/menu/create?access_token=%s";
    
    // 缓存的access_token
    private String accessToken = null;
    // access_token的过期时间（毫秒时间戳）
    private long accessTokenExpiresTime = 0;
    // 用于同步获取access_token的锁
    private final Lock accessTokenLock = new ReentrantLock();
    // 提前5分钟刷新token，避免临界点问题
    private static final long REFRESH_BEFORE_EXPIRE_MS = 5 * 60 * 1000;
    
    @Override
    public WxMpXmlOutMessage processMessage(WxMpXmlMessage inMessage) {
        if (inMessage == null) {
            log.warn("收到空消息");
            return createDefaultResponse();
        }
        
        // 获取发送消息用户的openid
        String fromUser = inMessage.getFromUser();
        
        // 检查用户是否已注册（即在系统中存在有效的用户记录且openid不为空）
        User user = userMapper.getUserByOpenid(fromUser);
        if (user == null) {
            log.info("收到未注册用户[{}]的消息，已忽略", fromUser);
            // 对于未注册用户，不响应其消息
            return null;
        }
        
        // 创建响应消息
        WxMpXmlOutMessage outMessage = WxMpXmlOutMessage.TEXT();
        outMessage.setToUser(inMessage.getFromUser());
        outMessage.setFromUser(inMessage.getToUser());
        
        // 处理不同类型的消息
        String msgType = inMessage.getMsgType();
        if (Constants.MessageType.TEXT.equals(msgType)) {
            return processTextMessage(inMessage, outMessage);
        } else if (Constants.MessageType.IMAGE.equals(msgType)) {
            return processImageMessage(inMessage);
        } else if (Constants.MessageType.EVENT.equals(msgType)) {
            return processEventMessage(inMessage, outMessage);
        } else {
            // 其他类型消息的默认响应
            outMessage.setContent("暂不支持此类型消息，请发送文本消息。");
            return outMessage;
        }
    }
    
    /**
     * 处理文本消息
     */
    private WxMpXmlOutMessage processTextMessage(WxMpXmlMessage inMessage, WxMpXmlOutMessage outMessage) {
        // 处理文本消息
        String content = inMessage.getContent() != null ? inMessage.getContent().trim() : "";
        
        // 简单的回声响应示例
        outMessage.setContent("您发送的消息是：" + content + "\n\n您可以通过查询指令获取任务信息。");
        return outMessage;
    }
    
    /**
     * 处理图片消息
     */
    private WxMpXmlOutMessage processImageMessage(WxMpXmlMessage inMessage) {
        // 创建图片类型回复
        WxMpXmlOutMessage outMessage = WxMpXmlOutMessage.IMAGE();
        outMessage.setToUser(inMessage.getFromUser());
        outMessage.setFromUser(inMessage.getToUser());
        
        // 使用收到的媒体ID直接回复相同的图片
        outMessage.setMediaId(inMessage.getMediaId());
        
        return outMessage;
    }
    
    /**
     * 处理事件消息
     */
    private WxMpXmlOutMessage processEventMessage(WxMpXmlMessage inMessage, WxMpXmlOutMessage outMessage) {
        // 处理事件消息
        String event = inMessage.getEvent();
        if (Constants.EventType.SUBSCRIBE.equals(event)) {
            // 新的订阅
            outMessage.setContent("感谢您关注日程提醒助手！\n\n您需要登录账号才能使用提醒功能。请访问我们的网站进行登录。");
        } else if (Constants.EventType.CLICK.equals(event)) {
            // 处理菜单点击事件
            String eventKey = inMessage.getEventKey();
            return processMenuClick(eventKey, outMessage);
        } else {
            // 其他事件
            outMessage.setContent("收到您的事件消息，谢谢！");
        }
        return outMessage;
    }
    
    /**
     * 处理菜单点击事件
     */
    private WxMpXmlOutMessage processMenuClick(String eventKey, WxMpXmlOutMessage outMessage) {
        if ("UPCOMING_TASKS".equals(eventKey)) {
            // 查询即将到来的任务
            outMessage.setContent("您即将到来的任务查询功能正在开发中，请稍后再试。");
        } else if ("MY_ACCOUNT".equals(eventKey)) {
            // 我的账户信息
            outMessage.setContent("您的账户信息查询功能正在开发中，请稍后再试。");
        } else if ("HELP".equals(eventKey)) {
            // 帮助信息
            outMessage.setContent("欢迎使用日程提醒助手！\n\n" +
                    "- 点击菜单查看您的日程\n" +
                    "- 登录网页版可以添加和管理日程\n" +
                    "- 系统会在任务开始前通过微信提醒您\n\n" +
                    "如需帮助，请联系管理员。");
        } else {
            // 未知的菜单项
            outMessage.setContent("您点击的功能暂不可用，请稍后再试。");
        }
        
        return outMessage;
    }
    
    /**
     * 创建默认响应消息
     */
    private WxMpXmlOutMessage createDefaultResponse() {
        WxMpXmlOutMessage outMessage = WxMpXmlOutMessage.TEXT();
        outMessage.setContent("系统繁忙，请稍后再试。");
        return outMessage;
    }
    
    @Override
    public boolean sendReminderMessage(String openId, String title, long startTime, long endTime, String location) {
        // 检查openId是否为空
        if (openId == null || openId.trim().isEmpty()) {
            log.warn("尝试发送提醒消息到空的openId，已跳过");
            return false;
        }
        
        try {
            // 获取访问令牌
            String accessToken = getAccessToken();
            if (accessToken == null) {
                log.error("获取访问令牌失败");
                return false;
            }
            
            log.info("成功获取访问令牌，准备发送模板消息");
            
            // 构建模板消息数据
            Map<String, Object> messageData = buildTemplateMessageData(
                openId, title, startTime, endTime, location);
            
            // 发送请求
            String url = String.format(API_SEND_TEMPLATE_URL, accessToken);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 将Map转换为JSON字符串
            String jsonBody;
            try {
                jsonBody = objectMapper.writeValueAsString(messageData);
            } catch (Exception e) {
                log.error("转换模板消息数据为JSON字符串失败: {}", e.getMessage());
                return false;
            }
            
            // 使用String作为请求体，确保JSON格式正确
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
            
            // 打印请求参数用于调试
            log.debug("发送模板消息请求URL: {}", url);
            log.debug("发送模板消息请求内容: {}", jsonBody);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            log.info("发送给{}的提醒消息，响应状态: {}, 内容: {}", openId, response.getStatusCode(), response.getBody());
            
            // 解析响应
            try {
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                if (responseMap.containsKey("errcode") && !responseMap.get("errcode").equals(0)) {
                    log.error("发送模板消息失败，错误码: {}, 错误信息: {}", 
                             responseMap.get("errcode"), responseMap.get("errmsg"));
                    return false;
                }
            } catch (Exception e) {
                log.warn("解析微信响应失败，但这不一定表示发送失败: {}", e.getMessage());
            }
            
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("发送提醒消息到{}失败: {}", openId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 构建模板消息数据
     */
    private Map<String, Object> buildTemplateMessageData(String openId, String title, 
                                                        long startTime, long endTime, String location) {
        // 格式化时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String timeStr = sdf.format(new Date(startTime)) + " - " + sdf.format(new Date(endTime));
        
        // 构建模板消息JSON
        Map<String, Object> msg = new HashMap<>();
        msg.put("touser", openId);
        msg.put("template_id", wechatMpConfig.getTemplates().getReminderTemplateId());
        msg.put("url", "");  // 用户点击时将重定向到的URL
        
        Map<String, Object> data = new HashMap<>();
        
        // 根据实际模板内容添加数据
        data.put("title", createDataItem(title, "#000000"));
        data.put("time", createDataItem(timeStr, "#000000"));
        data.put("location", createDataItem(location != null ? location : "无", "#000000"));
        data.put("content", createDataItem("请准时参加", "#000000"));
        data.put("remark", createDataItem("请做好准备，按时参加！", "#0000FF"));
        
        msg.put("data", data);
        
        // 打印最终构建的JSON数据用于调试
        try {
            log.debug("构建的模板消息JSON数据: {}", objectMapper.writeValueAsString(msg));
        } catch (Exception e) {
            log.warn("打印模板消息JSON数据失败", e);
        }
        
        return msg;
    }
    
    /**
     * 创建模板消息数据项
     */
    private Map<String, Object> createDataItem(String value, String color) {
        Map<String, Object> item = new HashMap<>();
        item.put("value", value);
        item.put("color", color);
        return item;
    }
    
    /**
     * 从微信API获取访问令牌（使用缓存机制）
     * 只有在令牌不存在或已过期时才请求新的令牌
     */
    private String getAccessToken() {
        long now = System.currentTimeMillis();
        
        // 检查当前的access_token是否存在且有效
        if (accessToken != null && now < (accessTokenExpiresTime - REFRESH_BEFORE_EXPIRE_MS)) {
            log.debug("使用缓存的访问令牌");
            return accessToken;
        }
        
        // 获取锁以避免并发请求
        accessTokenLock.lock();
        try {
            // 双重检查，以防另一个线程已经更新了token
            if (accessToken != null && now < (accessTokenExpiresTime - REFRESH_BEFORE_EXPIRE_MS)) {
                log.debug("使用已更新的访问令牌");
                return accessToken;
            }
            
            // 请求新的访问令牌
            return requestNewAccessToken();
        } finally {
            accessTokenLock.unlock();
        }
    }
    
    /**
     * 向微信API请求新的访问令牌
     * 
     * @return 新的访问令牌，如果请求失败返回null
     */
    private String requestNewAccessToken() {
        try {
            log.info("请求新的微信访问令牌");
            
            // 构建请求URL
            String url = String.format(API_GET_TOKEN_URL, 
                                       wechatMpConfig.getAppId(), 
                                       wechatMpConfig.getSecret());
            
            // 发送请求
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            log.debug("获取访问令牌响应: {}", response.getBody());
            
            // 解析响应
            Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);
            
            // 检查是否有错误
            if (result.containsKey("errcode") && !result.get("errcode").equals(0)) {
                log.error("获取访问令牌失败，错误码: {}, 错误信息: {}", 
                          result.get("errcode"), result.get("errmsg"));
                return null;
            }
            
            // 获取访问令牌和过期时间
            if (result.containsKey("access_token") && result.containsKey("expires_in")) {
                // 更新缓存的访问令牌
                this.accessToken = (String) result.get("access_token");
                
                // 计算过期时间（当前时间 + expires_in秒）
                // expires_in通常是7200（2小时）
                int expiresIn = ((Number) result.get("expires_in")).intValue();
                this.accessTokenExpiresTime = System.currentTimeMillis() + (expiresIn * 1000L);
                
                log.info("成功获取新的访问令牌，有效期: {}秒", expiresIn);
                return this.accessToken;
            } else {
                log.error("获取访问令牌响应格式异常: {}", response.getBody());
                return null;
            }
        } catch (Exception e) {
            log.error("请求访问令牌时发生异常: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean createCustomMenu() {
        try {
            // 获取访问令牌
            String accessToken = getAccessToken();
            if (accessToken == null) {
                log.error("创建自定义菜单失败：无法获取访问令牌");
                return false;
            }
            
            // 构建自定义菜单数据
            Map<String, Object> menu = buildCustomMenuData();
            
            // 将菜单数据转换为JSON
            String jsonMenu;
            try {
                jsonMenu = objectMapper.writeValueAsString(menu);
            } catch (Exception e) {
                log.error("转换菜单数据为JSON失败: {}", e.getMessage());
                return false;
            }
            
            // 发送请求创建菜单
            String url = String.format(API_CREATE_MENU_URL, accessToken);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(jsonMenu, headers);
            
            log.debug("创建自定义菜单请求: {}", jsonMenu);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            log.info("创建自定义菜单响应: {}", response.getBody());
            
            // 解析响应
            try {
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                if (responseMap.containsKey("errcode") && !responseMap.get("errcode").equals(0)) {
                    log.error("创建自定义菜单失败，错误码: {}, 错误信息: {}", 
                              responseMap.get("errcode"), responseMap.get("errmsg"));
                    return false;
                }
            } catch (Exception e) {
                log.error("解析创建自定义菜单响应失败: {}", e.getMessage());
                return false;
            }
            
            log.info("成功创建自定义菜单");
            return true;
        } catch (Exception e) {
            log.error("创建自定义菜单时发生异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 构建自定义菜单数据
     */
    private Map<String, Object> buildCustomMenuData() {
        // 构建菜单结构
        Map<String, Object> menu = new HashMap<>();
        List<Map<String, Object>> button = new ArrayList<>();
        
        // 任务菜单
        Map<String, Object> taskButton = new HashMap<>();
        taskButton.put("name", "我的任务");
        taskButton.put("type", "click");
        taskButton.put("key", "UPCOMING_TASKS");
        
        // 个人中心菜单
        Map<String, Object> accountButton = new HashMap<>();
        accountButton.put("name", "个人中心");
        accountButton.put("type", "click");
        accountButton.put("key", "MY_ACCOUNT");
        
        // 帮助菜单
        Map<String, Object> helpButton = new HashMap<>();
        helpButton.put("name", "使用帮助");
        helpButton.put("type", "click");
        helpButton.put("key", "HELP");
        
        // 添加按钮到菜单
        button.add(taskButton);
        button.add(accountButton);
        button.add(helpButton);
        
        menu.put("button", button);
        return menu;
    }

    /**
     * 构建文本回复消息
     * @param toUser 接收方OpenID
     * @param fromUser 发送方OpenID (公众号原始ID)
     * @param content 回复内容
     * @return XML格式的文本回复消息
     */
    @Override
    public String buildTextResponse(String toUser, String fromUser, String content) {
        long createTime = System.currentTimeMillis() / 1000;
        return "<xml>" +
                "<ToUserName><![CDATA[" + toUser + "]]></ToUserName>" +
                "<FromUserName><![CDATA[" + fromUser + "]]></FromUserName>" +
                "<CreateTime>" + createTime + "</CreateTime>" +
                "<MsgType><![CDATA[text]]></MsgType>" +
                "<Content><![CDATA[" + content + "]]></Content>" +
                "</xml>";
    }
} 