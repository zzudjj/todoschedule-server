package com.djj.todoscheduleserver.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 混合逻辑时钟结构，包含物理时间、逻辑计数器和节点ID
 */
@Data
@Schema(description = "混合逻辑时钟")
public class HybridLogicalClock {
    
    @Schema(description = "物理墙钟时间（毫秒）", required = true)
    private long wallClockTime;
    
    @Schema(description = "逻辑计数器", required = true)
    private int logicalCounter;
    
    @Schema(description = "节点ID", required = true)
    private String nodeId;
}
