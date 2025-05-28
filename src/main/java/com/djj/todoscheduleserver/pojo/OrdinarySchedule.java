package com.djj.todoscheduleserver.pojo;

import lombok.Data;
import java.sql.Timestamp;

/**
 * 普通日程实体类
 */
@Data
public class OrdinarySchedule {
    private String crdtKey;      // CRDT实体唯一键
    private Integer userId;
    private String title;
    private String description;
    private String location;
    private String category;
    private String color;
    private Boolean isAllDay;
    private String status;
    private String startTime;    // 日程开始时间 YYYY-MM-DD HH:MM:SS
    private String endTime;      // 日程结束时间 YYYY-MM-DD HH:MM:SS
    private Integer priority;
    private Boolean completed;
    
    // CRDT 相关字段
    private Long hlcTimestamp;   // 混合逻辑时钟时间戳
    private Boolean isDeleted;   // 软删除标记
    private Timestamp deletedAt; // 软删除时间
} 