package com.djj.todoscheduleserver.pojo;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class User {
    private Integer id;
    private String username;
    private String openid;
    private String phoneNumber;
    private String email;
    private String avatar;
    private Timestamp createdAt;
    private String passwordHash;
    private Timestamp lastOpen;
    private String token;
} 