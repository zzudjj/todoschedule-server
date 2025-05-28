package com.djj.todoscheduleserver.exception;

import org.springframework.http.HttpStatus;

/**
 * API错误异常类，用于表示API调用过程中的错误
 */
public class ApiError extends RuntimeException {
    
    private final HttpStatus status;
    private final int errorCode;
    private final String message;
    
    public ApiError(HttpStatus status, int errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
    }
    
    public HttpStatus getStatus() {
        return status;
    }
    
    public int getErrorCode() {
        return errorCode;
    }
    
    @Override
    public String getMessage() {
        return message;
    }
}
