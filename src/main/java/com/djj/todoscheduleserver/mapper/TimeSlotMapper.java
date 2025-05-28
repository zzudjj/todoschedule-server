package com.djj.todoscheduleserver.mapper;

import com.djj.todoscheduleserver.pojo.TimeSlot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;

/**
 * 时间槽Mapper接口
 */
@Mapper
public interface TimeSlotMapper {
    
    /**
     * 查询即将到来的任务
     */
    List<TimeSlot> findUpcomingTasks(@Param("startTimeMin") Long startTimeMin, 
                                     @Param("startTimeMax") Long startTimeMax);
    
    /**
     * 查询未通知的即将到来的任务
     */
    List<TimeSlot> findNonNotifiedUpcomingTasks(@Param("startTimeMin") Long startTimeMin, 
                                              @Param("startTimeMax") Long startTimeMax);
    
    /**
     * 标记为已通知
     */
    int markAsNotified(@Param("id") Integer id);
    
    /**
     * 获取普通日程标题
     */
    String getOrdinaryScheduleTitle(@Param("scheduleId") Integer scheduleId);
    
    /**
     * 获取普通日程位置
     */
    String getOrdinaryScheduleLocation(@Param("scheduleId") Integer scheduleId);
    
    /**
     * 获取课程标题
     */
    String getCourseTitle(@Param("courseNodeId") Integer courseNodeId);
    
    /**
     * 获取课程位置
     */
    String getCourseLocation(@Param("courseNodeId") Integer courseNodeId);

    /**
     * 根据ID获取时间槽
     */
    TimeSlot getById(@Param("id") Integer id);
    
    /**
     * 获取用户的所有时间槽（未删除的）
     */
    List<TimeSlot> getAllByUserId(@Param("userId") Integer userId);
    
    /**
     * 获取用户在指定时间范围内的时间槽（未删除的）
     */
    List<TimeSlot> getByTimeRange(
            @Param("userId") Integer userId, 
            @Param("startTime") Long startTime, 
            @Param("endTime") Long endTime
    );
    
    /**
     * 获取与特定日程或课程关联的所有时间槽（未删除的）
     */
    List<TimeSlot> getByScheduleCrdtKey(
            @Param("userId") Integer userId,
            @Param("scheduleType") String scheduleType,
            @Param("scheduleCrdtKey") String scheduleCrdtKey
    );
    
    /**
     * 获取用户在指定时间后更新的时间槽（包括已删除的）
     */
    List<TimeSlot> getUpdatedAfterTimestamp(
            @Param("userId") Integer userId, 
            @Param("hlcTimestamp") Long hlcTimestamp
    );
    
    /**
     * 新增或更新时间槽
     */
    int insertOrUpdate(TimeSlot timeSlot);
    
    /**
     * 标记时间槽为已删除
     */
    int markAsDeleted(
            @Param("crdtKey") String crdtKey, 
            @Param("hlcTimestamp") Long hlcTimestamp, 
            @Param("deletedAt") Timestamp deletedAt
    );
    
    /**
     * 获取需要提醒的时间槽
     */
    List<TimeSlot> getRemindersToSend(
            @Param("currentTime") Long currentTime
    );
    
    /**
     * 更新时间槽的提醒状态
     */
    int updateNotificationStatus(
            @Param("crdtKey") String crdtKey, 
            @Param("isNotified") Boolean isNotified
    );

    TimeSlot getByCrdtKey(@Param("crdtKey") String crdtKey);

    List<TimeSlot> getBySchedule(@Param("userId") Integer userId, @Param("scheduleType") String scheduleType, @Param("scheduleId") Integer scheduleId);
    
    List<TimeSlot> getUpcomingReminders(@Param("currentTimeMillis") Long currentTimeMillis, @Param("lookAheadMillis") Long lookAheadMillis);

    int insertOrUpdateCrdt(TimeSlot timeSlot);

    int markAsDeletedCrdt(@Param("userId") Integer userId, @Param("crdtKey") String crdtKey, @Param("hlcTimestamp") Long hlcTimestamp, @Param("deletedAt") java.sql.Timestamp deletedAt);

    int updateCompletionStatusCrdt(@Param("userId") Integer userId, @Param("crdtKey") String crdtKey, @Param("isCompleted") Boolean isCompleted, @Param("hlcTimestamp") Long hlcTimestamp);

    int updateNotificationStatusCrdt(@Param("userId") Integer userId, @Param("crdtKey") String crdtKey, @Param("isNotified") Boolean isNotified, @Param("hlcTimestamp") Long hlcTimestamp);
} 