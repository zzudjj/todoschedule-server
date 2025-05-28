package com.djj.todoscheduleserver.mapper;

import com.djj.todoscheduleserver.pojo.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {
    
    @Select("SELECT * FROM user WHERE id = #{id}")
    User getUserById(@Param("id") Integer id);
    
    @Select("SELECT * FROM user WHERE openid = #{openid}")
    User getUserByOpenid(@Param("openid") String openid);
    
    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM user WHERE username = #{username}")
    User getUserByUsername(@Param("username") String username);
    
    /**
     * 根据token查询用户
     */
    @Select("SELECT * FROM user WHERE token = #{token}")
    User getUserByToken(@Param("token") String token);
    
    /**
     * 插入新用户
     */
    @Insert("INSERT INTO user (username, openid, phone_number, email, avatar, created_at, password_hash, token) " +
            "VALUES (#{username}, #{openid}, #{phoneNumber}, #{email}, #{avatar}, #{createdAt}, #{passwordHash}, #{token})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(User user);
    
    /**
     * 更新用户所有信息
     */
    @Update("<script>" +
            "UPDATE user " +
            "<set>" +
            "<if test='username != null'>username = #{username},</if>" +
            "<if test='openid != null'>openid = #{openid},</if>" +
            "<if test='phoneNumber != null'>phone_number = #{phoneNumber},</if>" +
            "<if test='email != null'>email = #{email},</if>" +
            "<if test='avatar != null'>avatar = #{avatar},</if>" +
            "<if test='passwordHash != null'>password_hash = #{passwordHash},</if>" +
            "<if test='lastOpen != null'>last_open = #{lastOpen},</if>" +
            "<if test='token != null'>token = #{token},</if>" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    int updateUser(User user);
    
    /**
     * 更新用户openid
     */
    @Update("UPDATE user SET openid = #{openid} WHERE id = #{id}")
    int updateUserOpenid(@Param("id") Integer id, @Param("openid") String openid);
    
    /**
     * 更新用户上次登录时间
     */
    @Update("UPDATE user SET last_open = #{lastOpen} WHERE id = #{id}")
    int updateUserLastOpen(@Param("id") Integer id, @Param("lastOpen") java.sql.Timestamp lastOpen);
} 