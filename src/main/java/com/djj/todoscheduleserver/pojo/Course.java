package com.djj.todoscheduleserver.pojo;

import lombok.Data;
import java.sql.Timestamp;

/**
 * 课程实体类
 */
@Data
public class Course {
    private String crdtKey;      // CRDT实体唯一键
    private Integer userId;
    private String courseName;
    private String color;
    private String room;
    private String teacher;
    private Float credit;
    private String courseCode;
    private String syllabusLink;
    private Integer startNode;
    private Integer step;
    private Integer day;
    private Integer startWeek;
    private Integer endWeek;
    private Integer weekType;
    
    // CRDT 相关字段
    private Long hlcTimestamp;   // 混合逻辑时钟时间戳
    private Boolean isDeleted;   // 软删除标记
    private Timestamp deletedAt; // 软删除时间
} 