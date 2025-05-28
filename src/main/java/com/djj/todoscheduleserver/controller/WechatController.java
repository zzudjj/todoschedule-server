package com.djj.todoscheduleserver.controller;

import com.djj.todoscheduleserver.config.WechatMpConfig;
import com.djj.todoscheduleserver.pojo.User;
import com.djj.todoscheduleserver.service.SyncService;
import com.djj.todoscheduleserver.service.UserService;
import com.djj.todoscheduleserver.service.WechatService;
import com.djj.todoscheduleserver.utils.WechatUtils;
import com.djj.todoscheduleserver.utils.XmlUtils;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 微信控制器
 * 处理微信服务器验证和消息交互
 */
@Slf4j
@RestController
@RequestMapping("/wechat")
@Hidden // 通常，直接消息处理可能不属于公共API文档的一部分
public class WechatController {

    @Autowired
    private WechatService wechatService;
    
    @Autowired
    private WechatMpConfig wechatMpConfig;
    
    @Autowired
    private UserService userService;

    @Autowired
    private SyncService syncService; // 用于CRDT处理的新SyncService
    
    /**
     * 处理微信服务器验证请求的GET请求
     * 使用适用于测试账号的方法
     */
    @GetMapping
    @Operation(summary = "验证微信服务器", description = "处理微信服务器发送的验证请求")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "验证成功"),
        @ApiResponse(responseCode = "401", description = "验证失败")
    })
    public String verifyServer(
            @Parameter(description = "微信加密签名") @RequestParam(value = "signature", required = false) String signature,
            @Parameter(description = "时间戳") @RequestParam(value = "timestamp", required = false) String timestamp,
            @Parameter(description = "随机数") @RequestParam(value = "nonce", required = false) String nonce,
            @Parameter(description = "回显字符串") @RequestParam(value = "echostr", required = false) String echostr) {
        log.info("收到验证请求: signature={}, timestamp={}, nonce={}, echostr={}", 
                signature, timestamp, nonce, echostr);
                
        // 验证签名
        if (WechatUtils.checkSignature(signature, timestamp, nonce, wechatMpConfig.getToken())) {
            return echostr;
        } else {
            return null;
        }
    }
    
    /**
     * 处理接收微信消息的POST请求
     */
    @PostMapping(value = "/message", consumes = {"application/xml", "text/xml"}, produces = "application/xml;charset=utf-8")
    @Operation(summary = "接收微信消息", description = "处理微信服务器推送的消息和事件")
    public String handleMessage(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"))) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception e) {
            log.error("读取微信请求体失败: {}", e.getMessage());
            return "success"; // 微信在出错时也期望返回 "success" 或空字符串
        }

        String requestBody = sb.toString();
        log.info("接收到微信消息: {}", requestBody);

        try {
            Document document = DocumentHelper.parseText(requestBody);
            Element root = document.getRootElement();
            String fromUserName = root.elementText("FromUserName"); // 用户的OpenID
            String toUserName = root.elementText("ToUserName");   // 你的应用的原始ID
            String msgType = root.elementText("MsgType");

            if ("text".equals(msgType)) {
                String content = root.elementText("Content");
                log.info("微信文本消息内容 [来自: {}, 内容: {}]", fromUserName, content);

                User user = userService.getUserByOpenid(fromUserName);
                if (user != null) {
                    // 在纯中继模式下，服务器不再处理微信消息转换为日程的功能
                    log.info("收到用户 {} 的微信日程消息，但在纯中继模式下无法处理：{}", user.getId(), content);
                    return wechatService.buildTextResponse(fromUserName, toUserName, 
                        "服务器已升级为纯CRDT消息中继模式，不再支持通过微信直接添加日程。请使用客户端APP添加日程。");
                } else {
                    log.warn("未找到与OpenID {} 关联的用户，无法处理日程消息。", fromUserName);
                    return wechatService.buildTextResponse(fromUserName, toUserName, "请先在APP中登录并绑定微信账号，才能通过公众号添加日程哦。");
                }
            } else if ("event".equals(msgType)) {
                String event = root.elementText("Event");
                if ("subscribe".equalsIgnoreCase(event)) {
                    log.info("用户 {} 关注公众号", fromUserName);
                    return wechatService.buildTextResponse(fromUserName, toUserName, "欢迎关注！本服务支持在多设备间同步您的日程和待办事项。");
                }
                // 处理其他事件，如取消关注等。
            }
            // 对于其他消息类型或未处理的事件，微信期望返回空字符串或 "success"
            return "success";

        } catch (Exception e) {
            log.error("处理微信消息失败: {}", e.getMessage(), e);
            // 即使出错，也要回复微信以防止重试
            // 如果可能，尝试构建通用的失败响应，否则返回success
            try {
                Document tempDoc = DocumentHelper.parseText(requestBody); // 如果需要，重新解析
                Element tempRoot = tempDoc.getRootElement();
                String fromUser = tempRoot.elementText("FromUserName");
                String toUser = tempRoot.elementText("ToUserName");
                return wechatService.buildTextResponse(fromUser, toUser, "抱歉，处理您的消息时遇到一点问题。");
            } catch (Exception ex) {
                return "success";
            }
        }
    }
    
    /**
     * 用于发送提醒消息的测试端点（用于开发/测试）
     */
    @GetMapping("/test-reminder")
    @Operation(summary = "测试提醒消息", description = "发送测试提醒消息到指定用户")
    public String testReminderMessage(
            @Parameter(description = "用户的微信OpenID") @RequestParam("openid") String openId) {
        long now = System.currentTimeMillis();
        long startTime = now + 15 * 60 * 1000; // 15分钟后
        long endTime = startTime + 60 * 60 * 1000; // 开始后1小时
        
        boolean success = wechatService.sendReminderMessage(
            openId, 
            "测试任务提醒", 
            startTime, 
            endTime, 
            "测试地点"
        );
        
        return success ? "提醒发送成功" : "提醒发送失败";
    }
    
    /**
     * 创建微信公众号自定义菜单
     */
    @PostMapping("/create-menu")
    @Operation(summary = "创建自定义菜单", description = "为公众号创建自定义菜单")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "500", description = "创建失败")
    })
    public String createCustomMenu() {
        boolean success = wechatService.createCustomMenu();
        return success ? "自定义菜单创建成功" : "自定义菜单创建失败";
    }
} 