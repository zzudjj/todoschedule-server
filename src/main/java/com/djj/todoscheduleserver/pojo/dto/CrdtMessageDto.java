package com.djj.todoscheduleserver.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * CRDT消息数据传输对象
 */
@Data
@Schema(description = "CRDT消息数据传输对象")
public class CrdtMessageDto {
    
    @Schema(description = "实体的全局唯一标识符", required = true)
    private String crdtKey;
    
    @Schema(description = "混合逻辑时钟完整结构")
    private HybridLogicalClock hlc;
    
    @Schema(description = "产生此消息的设备ID", required = true)
    private String originDeviceId;
    
    @Schema(description = "实体是否已被删除")
    private boolean isDeleted;
    
    @Schema(description = "操作类型，可为 ADD、UPDATE 或 DELETE", required = true)
    private String operationType;
    
    @Schema(description = "实体数据，包含所有业务字段", required = true)
    private Map<String, Object> data;
}
