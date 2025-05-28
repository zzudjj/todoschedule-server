package com.djj.todoscheduleserver.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 批量上传CRDT消息的响应
 */
@Data
@Schema(description = "批量上传CRDT消息的响应")
public class BatchUploadResponseDto {
    
    @Schema(description = "上传是否成功")
    private boolean success;
    
    @Schema(description = "接收到的消息数量")
    private int messagesReceived;
    
    @Schema(description = "各条消息的处理结果")
    private List<MessageResult> results;
    
    @Schema(description = "错误码，当success为false时提供")
    private Integer errorCode;
    
    @Schema(description = "错误信息，当success为false时提供")
    private String errorMessage;
    
    /**
     * 单条消息处理结果
     */
    @Data
    @Schema(description = "单条消息处理结果")
    public static class MessageResult {
        
        @Schema(description = "消息的crdtKey")
        private String crdtKey;
        
        @Schema(description = "消息处理状态: success, failure")
        private String status;
        
        @Schema(description = "错误信息，当status为failure时提供")
        private String message;
    }
}
