package com.djj.todoscheduleserver.pojo;

import lombok.Data;
import java.sql.Timestamp;

/**
 * 时间槽实体类
 */
@Data
public class TimeSlot {
    private String crdtKey;       // CRDT实体唯一键
    private Integer userId;
    private Long startTime;
    private Long endTime;
    private String scheduleType;
    private String scheduleCrdtKey; // 关联的日程或课程CRDT键
    private String head;
    private Integer priority;
    private Boolean isCompleted;
    private Boolean isRepeated;
    private String repeatPattern;
    private String reminderType;
    private Long reminderOffset;
    private Boolean isNotified;
    
    // CRDT 相关字段
    private Long hlcTimestamp;    // 混合逻辑时钟时间戳
    private Boolean isDeleted;    // 软删除标记
    private Timestamp deletedAt;  // 软删除时间
} 