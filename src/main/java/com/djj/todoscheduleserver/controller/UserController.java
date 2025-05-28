package com.djj.todoscheduleserver.controller;

import com.djj.todoscheduleserver.common.Result;
import com.djj.todoscheduleserver.pojo.User;
import com.djj.todoscheduleserver.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户模块接口
 * 处理用户注册、登录、信息管理
 */
@Slf4j
@RestController
@RequestMapping("/users")
@Tag(name = "用户接口", description = "处理用户注册、登录、信息管理")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "注册成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    public Result<Map<String, Object>> register(@RequestBody User user) {
        // 参数校验
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return Result.badRequest("用户名不能为空");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().trim().isEmpty()) {
            log.info(user.toString());
            return Result.badRequest("密码不能为空");
        }
        
        // 检查用户名是否已存在
        if (userService.getUserByUsername(user.getUsername()) != null) {
            return Result.badRequest("用户名已存在");
        }
        
        // 设置创建时间
        user.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        
        // 保存用户 - 密码将在UserService中被哈希处理
        User newUser = userService.createUser(user);
        if (newUser == null) {
            return Result.serverError("注册失败，请稍后重试");
        }
        
        // 生成token
        String token = userService.generateToken(newUser);
        
        // 构建响应
        Map<String, Object> data = new HashMap<>();
        data.put("id", newUser.getId());
        data.put("username", newUser.getUsername());
        data.put("token", token);
        data.put("created_at", newUser.getCreatedAt());
        
        return Result.success("注册成功", data);
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录并获取令牌")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "登录成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "用户名或密码错误")
    })
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");  // 明文密码
        log.info("登录验证: ", username);
        // 参数校验
        if (username == null || username.trim().isEmpty()) {
            return Result.badRequest("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.badRequest("密码不能为空");
        }
        
        log.info("登录验证: username={}, 尝试登录", username);
        
        // 验证用户名和密码
        User user = userService.validateCredentials(username, password);
        if (user == null) {
            return Result.unauthorized("用户名或密码错误");
        }
        
        // 更新上次登录时间
        user.setLastOpen(new Timestamp(System.currentTimeMillis()));
        userService.updateUser(user);
        
        // 生成新的访问令牌
        String token = user.getToken();
        if (user.getToken() == null) {
            token = userService.generateToken(user);
        }

        // 构建响应
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("token", token);
        data.put("last_open", user.getLastOpen());
        
        return Result.success("登录成功", data);
    }
    
    /**
     * 获取用户信息
     */
    @GetMapping("/profile")
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "未认证或认证已过期")
    })
    public Result<User> getUserProfile(@RequestHeader("Authorization") String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Result.unauthorized("未提供有效的授权信息");
        }
        
        String token = authorization.substring(7); // 去掉"Bearer "前缀
        User user = userService.getUserByToken(token);
        
        if (user == null) {
            return Result.unauthorized("无效的令牌或令牌已过期");
        }
        
        // 移除敏感信息
        user.setPasswordHash(null);
        
        return Result.success(user);
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/profile")
    @Operation(summary = "更新用户信息", description = "更新当前登录用户的信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证或认证已过期")
    })
    public Result<User> updateUserProfile(@RequestHeader("Authorization") String authorization, 
                                          @RequestBody User updatedUser) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Result.unauthorized("未提供有效的授权信息");
        }
        
        String token = authorization.substring(7); // 去掉"Bearer "前缀
        User currentUser = userService.getUserByToken(token);
        
        if (currentUser == null) {
            return Result.unauthorized("无效的令牌或令牌已过期");
        }
        
        // 设置ID以确保只更新当前用户
        updatedUser.setId(currentUser.getId());
        
        // 不允许更新某些字段
        updatedUser.setPasswordHash(null);
        updatedUser.setToken(null);
        updatedUser.setCreatedAt(null);
        
        // 更新用户
        User user = userService.updateUserProfile(updatedUser);
        if (user == null) {
            return Result.serverError("更新失败，请稍后重试");
        }
        
        // 移除敏感信息
        user.setPasswordHash(null);
        
        return Result.success("用户信息更新成功", user);
    }
    
    /**
     * 修改密码
     */
    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "修改当前登录用户的密码")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "修改成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证或认证已过期")
    })
    public Result<?> changePassword(@RequestHeader("Authorization") String authorization,
                                   @RequestBody Map<String, String> passwordData) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Result.unauthorized("未提供有效的授权信息");
        }
        
        String token = authorization.substring(7); // 去掉"Bearer "前缀
        User currentUser = userService.getUserByToken(token);
        
        if (currentUser == null) {
            return Result.unauthorized("无效的令牌或令牌已过期");
        }
        
        String oldPassword = passwordData.get("old_password");
        String newPassword = passwordData.get("new_password");
        
        // 参数校验
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            return Result.badRequest("旧密码不能为空");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return Result.badRequest("新密码不能为空");
        }
        
        // 验证旧密码
        boolean isOldPasswordValid = userService.validatePassword(currentUser, oldPassword);
        if (!isOldPasswordValid) {
            return Result.badRequest("旧密码错误");
        }
        
        // 更新密码
        boolean isUpdated = userService.updatePassword(currentUser.getId(), newPassword);
        if (!isUpdated) {
            return Result.serverError("密码修改失败，请稍后重试");
        }
        
        return Result.success("密码修改成功");
    }
} 