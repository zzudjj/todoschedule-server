package com.djj.todoscheduleserver.service;

import com.djj.todoscheduleserver.pojo.User;

/**
 * 用户服务接口
 * 处理用户相关的业务逻辑
 */
public interface UserService {
    
    /**
     * 根据ID获取用户
     */
    User getUserById(Integer id);
    
    /**
     * 根据用户名获取用户
     */
    User getUserByUsername(String username);
    
    /**
     * 根据OpenID获取用户
     */
    User getUserByOpenid(String openid);
    
    /**
     * 根据令牌获取用户
     */
    User getUserByToken(String token);
    
    /**
     * 创建新用户
     */
    User createUser(User user);
    
    /**
     * 更新用户
     */
    int updateUser(User user);
    
    /**
     * 更新用户配置文件
     */
    User updateUserProfile(User user);
    
    /**
     * 验证用户凭据
     */
    User validateCredentials(String username, String password);
    
    /**
     * 验证用户密码
     */
    boolean validatePassword(User user, String password);
    
    /**
     * 更新用户密码
     */
    boolean updatePassword(Integer userId, String newPassword);
    
    /**
     * 生成用户访问令牌
     */
    String generateToken(User user);
} 