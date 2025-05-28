package com.djj.todoscheduleserver.common;

import lombok.Data;

/**
 * 统一响应格式封装类
 * 用于统一所有API接口的响应格式
 */
@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    
    private Result() {
    }
    
    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    /**
     * 请求成功(带数据)
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }
    
    /**
     * 请求成功(带消息和数据)
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }
    
    /**
     * 请求成功(只带消息)
     */
    public static <T> Result<T> success(String message) {
        return new Result<>(200, message, null);
    }
    
    /**
     * 请求失败
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
    
    /**
     * 请求失败(参数错误)
     */
    public static <T> Result<T> badRequest(String message) {
        return new Result<>(400, message, null);
    }
    
    /**
     * 请求失败(未认证)
     */
    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(401, message, null);
    }
    
    /**
     * 请求失败(权限不足)
     */
    public static <T> Result<T> forbidden(String message) {
        return new Result<>(403, message, null);
    }
    
    /**
     * 请求失败(资源不存在)
     */
    public static <T> Result<T> notFound(String message) {
        return new Result<>(404, message, null);
    }
    
    /**
     * 请求失败(服务器内部错误)
     */
    public static <T> Result<T> serverError(String message) {
        return new Result<>(500, message, null);
    }
} 