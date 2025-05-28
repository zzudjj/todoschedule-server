package com.djj.todoscheduleserver.pojo.sync;

import lombok.Data;
import java.sql.Timestamp;

/**
 * 课程同步数据类
 */
@Data
public class CourseData {
    private Integer id;
    private Integer tableId;
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
    private Timestamp updatedAt;
    private Timestamp createdAt;
} 