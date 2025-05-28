package com.djj.todoscheduleserver.controller;

import com.djj.todoscheduleserver.config.WechatMpConfig;
import com.djj.todoscheduleserver.mapper.UserMapper;
import com.djj.todoscheduleserver.pojo.User;
import com.djj.todoscheduleserver.service.UserService;
import com.djj.todoscheduleserver.service.WechatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信网页授权控制器
 * 处理微信网页授权获取openid的流程
 */
@Slf4j
@Controller
@RequestMapping("/oauth")
@Tag(name = "微信网页授权", description = "处理微信网页授权获取用户OpenID的流程")
public class WechatOAuthController {

    @Autowired
    private WechatMpConfig wechatMpConfig;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserService userService;
    
    @Value("${wechat.server-url}")
    private String serverUrl;
    
    @Value("${wechat.oauth.success-page}")
    private String oauthSuccessPage;
    
    private static final String OAUTH2_AUTHORIZE_URL = "https://open.weixin.qq.com/connect/oauth2/authorize";
    private static final String OAUTH2_ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";
    
    /**
     * 第一步：引导用户进入授权页面
     * 重定向到微信授权页面
     */
    @GetMapping("/authorize")
    @Operation(summary = "引导用户授权", description = "重定向用户到微信授权页面，用于获取授权码")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "302", description = "重定向到微信授权页面")
    })
    public String authorize(
            @Parameter(description = "授权后的重定向地址") 
            @RequestParam(value = "redirect_url", required = false) String redirectUrl) 
            throws UnsupportedEncodingException {
        if (redirectUrl == null || redirectUrl.isEmpty()) {
            redirectUrl = serverUrl; // 默认重定向到网站首页
        }
        
        // 构建回调URL
        String callbackUrl = serverUrl + "/oauth/callback";
        String encodedCallbackUrl = URLEncoder.encode(callbackUrl, "UTF-8");
        
        // 构建授权URL
        String authUrl = UriComponentsBuilder.fromHttpUrl(OAUTH2_AUTHORIZE_URL)
                .queryParam("appid", wechatMpConfig.getAppId())
                .queryParam("redirect_uri", encodedCallbackUrl)
                .queryParam("response_type", "code")
                .queryParam("scope", "snsapi_base") // 静默授权，只获取openid
                .queryParam("state", URLEncoder.encode(redirectUrl, "UTF-8"))
                .build().toUriString();
        
        authUrl += "#wechat_redirect"; // 必须加上这个微信特殊参数
        
        log.info("重定向到微信授权URL: {}", authUrl);
        return "redirect:" + authUrl;
    }
    
    /**
     * 验证用户名和密码
     */
    @PostMapping("/verify-credentials")
    @ResponseBody
    @Operation(summary = "验证用户凭据", description = "验证用户名和密码，并将用户与微信OpenID关联")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "验证结果，包含成功或失败信息")
    })
    public Map<String, Object> verifyCredentials(
            @Parameter(description = "用户名") @RequestParam("username") String username, 
            @Parameter(description = "密码") @RequestParam("password") String password,
            @Parameter(description = "微信OpenID") @RequestParam("openid") String openid) {
        Map<String, Object> response = new HashMap<>();
        
        log.info("微信授权验证: username={}, password长度={}, openid={}", username, 
                password != null ? password.length() : 0, openid);
        
        // 使用UserService验证用户名和密码
        User user = userService.validateCredentials(username, password);
        
        if (user != null) {
            // 验证成功，更新用户openid
            user.setOpenid(openid);
            user.setLastOpen(Timestamp.from(Instant.now()));
            userService.updateUser(user);
            log.info("更新用户openid: userId={}, username={}, openid={}", user.getId(), username, openid);
            
            response.put("success", true);
            response.put("userId", user.getId());
        } else {
            response.put("success", false);
            response.put("message", "用户名或密码错误");
            log.warn("用户名或密码验证失败: username={}", username);
        }
        
        return response;
    }
    
    /**
     * 第二步：处理授权回调，获取code并换取access_token和openid
     */
    @GetMapping("/callback")
    @Operation(summary = "处理授权回调", description = "接收微信授权回调，用授权码换取用户的OpenID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "302", description = "重定向到携带OpenID的成功页面")
    })
    public String handleCallback(
            @Parameter(description = "授权码") @RequestParam("code") String code, 
            @Parameter(description = "状态参数，包含重定向URL") @RequestParam("state") String state,
            RedirectAttributes redirectAttributes) {
        log.info("收到微信授权回调，code={}, state={}", code, state);
        
        try {
            // 通过code换取网页授权access_token和openid
            String url = UriComponentsBuilder.fromHttpUrl(OAUTH2_ACCESS_TOKEN_URL)
                    .queryParam("appid", wechatMpConfig.getAppId())
                    .queryParam("secret", wechatMpConfig.getSecret())
                    .queryParam("code", code)
                    .queryParam("grant_type", "authorization_code")
                    .build().toUriString();
            
            // 使用String类型接收响应，避免内容类型不匹配问题
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            // 手动解析JSON响应
            Map<String, Object> result;
            try {
                result = objectMapper.readValue(response.getBody(), Map.class);
            } catch (Exception e) {
                log.error("解析微信响应失败: {}", response.getBody(), e);
                return "redirect:" + state + "?error=parse_response_error";
            }
            
            if (result != null && result.containsKey("openid")) {
                String openid = (String) result.get("openid");
                log.info("成功获取openid: {}", openid);
                
                // 将openid参数传递给重定向URL
                // 实际的用户验证和openid绑定将由前端调用/verify-credentials接口完成
                redirectAttributes.addAttribute("openid", openid);
                
                // 重定向到前端页面
                return "redirect:" + state;
            } else {
                log.error("获取openid失败: {}", result);
                return "redirect:" + state + "?error=failed_to_get_openid";
            }
        } catch (Exception e) {
            log.error("处理微信授权回调时出错", e);
            return "redirect:" + state + "?error=auth_error";
        }
    }
} 