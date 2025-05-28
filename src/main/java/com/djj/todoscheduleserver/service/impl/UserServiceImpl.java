package com.djj.todoscheduleserver.service.impl;

import com.djj.todoscheduleserver.mapper.UserMapper;
import com.djj.todoscheduleserver.pojo.User;
import com.djj.todoscheduleserver.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * 用户服务实现类
 * 实现用户相关的业务逻辑
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    
    @Override
    public User getUserById(Integer id) {
        return userMapper.getUserById(id);
    }
    
    @Override
    public User getUserByUsername(String username) {
        return userMapper.getUserByUsername(username);
    }
    
    @Override
    public User getUserByOpenid(String openid) {
        return userMapper.getUserByOpenid(openid);
    }
    
    @Override
    public User getUserByToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        
        // 实际项目中应该有一个token表来存储和管理token
        // 这里简化处理，假设token存储在用户表中
        User user = userMapper.getUserByToken(token);
        
        // 检查token是否过期
        // TODO: 实现token过期机制
        
        return user;
    }
    
    @Override
    public User createUser(User user) {
        try {
            // 使用BCrypt加密密码
            String plainPassword = user.getPasswordHash(); // 此处存储的是明文密码
            String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
            user.setPasswordHash(hashedPassword);
            
            // 生成默认token
            user.setToken(generateToken(user));
            
            // 设置创建时间（如果未设置）
            if (user.getCreatedAt() == null) {
                user.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            }
            
            // 保存用户
            int result = userMapper.insertUser(user);
            
            if (result > 0) {
                return user;
            } else {
                log.error("创建用户失败: {}", user.getUsername());
                return null;
            }
        } catch (Exception e) {
            log.error("创建用户时发生错误: {}", e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public int updateUser(User user) {
        return userMapper.updateUser(user);
    }
    
    @Override
    public User updateUserProfile(User user) {
        try {
            int result = userMapper.updateUser(user);
            
            if (result > 0) {
                return userMapper.getUserById(user.getId());
            } else {
                log.error("更新用户资料失败: id={}", user.getId());
                return null;
            }
        } catch (Exception e) {
            log.error("更新用户资料时发生错误: {}", e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public User validateCredentials(String username, String password) {
        User user = userMapper.getUserByUsername(username);
        
        if (user == null) {
            log.info("用户名不存在: {}", username);
            return null;
        }
        
        if (validatePassword(user, password)) {
            return user;
        } else {
            log.info("密码验证失败: username={}", username);
            return null;
        }
    }
    
    @Override
    public boolean validatePassword(User user, String plainPassword) {
        if (user == null || user.getPasswordHash() == null || plainPassword == null) {
            return false;
        }
        
        try {
            // 使用BCrypt验证密码
            return BCrypt.checkpw(plainPassword, user.getPasswordHash());
        } catch (Exception e) {
            log.error("验证密码时发生错误: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean updatePassword(Integer userId, String newPassword) {
        try {
            User user = userMapper.getUserById(userId);
            if (user == null) {
                return false;
            }
            
            // 使用BCrypt生成新的密码哈希
            String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            user.setPasswordHash(hashedPassword);
            
            int result = userMapper.updateUser(user);
            return result > 0;
        } catch (Exception e) {
            log.error("更新密码时发生错误: userId={}, {}", userId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public String generateToken(User user) {
        if (user == null || user.getId() == null) {
            return null;
        }
        
        // 生成一个随机的UUID作为token
        String token = UUID.randomUUID().toString();
        
        // 更新用户的token
        User userToUpdate = new User();
        userToUpdate.setId(user.getId());
        userToUpdate.setToken(token);
        userMapper.updateUser(userToUpdate);
        
        return token;
    }
} 