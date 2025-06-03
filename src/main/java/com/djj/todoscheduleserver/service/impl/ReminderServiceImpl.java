package com.djj.todoscheduleserver.service.impl;

import com.djj.todoscheduleserver.mapper.*;
import com.djj.todoscheduleserver.pojo.Course;
import com.djj.todoscheduleserver.pojo.OrdinarySchedule;
import com.djj.todoscheduleserver.pojo.TimeSlot;
import com.djj.todoscheduleserver.pojo.User;
import com.djj.todoscheduleserver.pojo.crdt.SyncMessage;
import com.djj.todoscheduleserver.service.HlcService;
import com.djj.todoscheduleserver.service.ReminderService;
import com.djj.todoscheduleserver.service.WechatService;
import com.djj.todoscheduleserver.utils.Constants.ScheduleType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * 提醒服务实现类
 * 定时检查即将开始的任务并发送提醒
 * 注意：服务器现在是纯CRDT消息中继，提醒服务仅保存提醒消息，不进行其他处理
 */
@Slf4j
@Service
public class ReminderServiceImpl implements ReminderService {

    @Autowired
    private TimeSlotMapper timeSlotMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private WechatService wechatService;
    
    @Autowired
    private OrdinaryScheduleMapper ordinaryScheduleMapper;
    
    @Autowired
    private CourseMapper courseMapper;
    
    @Autowired
    private SyncMessageMapper syncMessageMapper;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private HlcService hlcService;
    
    @Value("${task.reminder.minutes-before:15}")
    private int minutesBefore; // 提前多少分钟提醒
    
    private static final long REMINDER_CHECK_INTERVAL_MS = 60 * 1000; // 1分钟，提醒检查间隔
    private static final long REMINDER_LOOKAHEAD_WINDOW_MS = 5 * 60 * 1000; // 从提醒时间开始，检查未来5分钟的任务

    /**
     * 每分钟运行一次，检查即将开始的任务
     */
    @Scheduled(fixedRate = REMINDER_CHECK_INTERVAL_MS) // 每分钟检查一次
    @Override
    @Transactional
    public void checkAndSendReminders() {
        long currentTimeMillis = System.currentTimeMillis();
        // getUpcomingReminders 查询已经考虑了 (start_time - reminder_offset)
        List<TimeSlot> upcomingTimeSlots = timeSlotMapper.getUpcomingReminders(currentTimeMillis, REMINDER_LOOKAHEAD_WINDOW_MS);

        if (!upcomingTimeSlots.isEmpty()) {
            log.info("发现 {} 个即将到来的提醒任务。", upcomingTimeSlots.size());
        }

        for (TimeSlot timeSlot : upcomingTimeSlots) {
//            if (timeSlot.getIsDeleted() != null && timeSlot.getIsDeleted()) continue; // 跳过软删除的
//            if (timeSlot.getIsCompleted() != null && timeSlot.getIsCompleted()) continue; // 跳过已完成的
//            if (timeSlot.getIsNotified() != null && timeSlot.getIsNotified()) continue; // 跳过已通知的
//
//            log.info("即将发送消息");
//            // 在应用逻辑中再次精确检查提醒时间
//            if (timeSlot.getStartTime() != null && timeSlot.getReminderOffset() != null) {
//                long reminderTime = timeSlot.getStartTime() - timeSlot.getReminderOffset();
//                log.info("即将发送消息");
//                log.info("currentTimeMillis = {}, reminderTime = {} \n currentTimeMillis = {}, reminderTime + REMINDER_LOOKAHEAD_WINDOW_MS = {}", currentTimeMillis, reminderTime, currentTimeMillis, reminderTime + REMINDER_LOOKAHEAD_WINDOW_MS);
//                if (currentTimeMillis >= reminderTime && currentTimeMillis < reminderTime + REMINDER_LOOKAHEAD_WINDOW_MS) {
//                    log.info("即将发送消息");
                    sendReminderForTimeSlot(timeSlot);
//                }
//            }
        }
    }
    
    /**
     * 为单个时间段发送任务提醒
     */
    private void sendReminderForTimeSlot(TimeSlot timeSlot) {
        User user = userMapper.getUserById(timeSlot.getUserId());
        if (user == null || user.getOpenid() == null || user.getOpenid().isEmpty()) {
            log.warn("TimeSlot CRDTKey {} 的用户ID {} 未找到或没有OpenID。跳过提醒。", timeSlot.getCrdtKey(), timeSlot.getUserId());
            return;
        }

        String title = getTaskTitle(timeSlot);
        String location = getTaskLocation(timeSlot);

        if (title == null || title.trim().isEmpty()) {
            log.warn("TimeSlot CRDTKey {} 的任务标题为空。跳过提醒。", timeSlot.getCrdtKey());
            return;
        }

        log.info("尝试为TimeSlot CRDTKey: {} 向用户: {} 发送提醒", timeSlot.getCrdtKey(), user.getUsername());

        boolean sent = wechatService.sendReminderMessage(
                user.getOpenid(),
                title,
                timeSlot.getStartTime(),
                timeSlot.getEndTime(),
                location
        );

        if (sent) {
            log.info("已为TimeSlot CRDTKey: {} 发送提醒", timeSlot.getCrdtKey());
            // 标记为已通知，并为此更改生成CRDT消息
            try {
                long newHlc = hlcService.now(user.getId());
                // 更新本地状态
                timeSlotMapper.updateNotificationStatusCrdt(user.getId(), timeSlot.getCrdtKey(), true, newHlc);
                
                // 创建CRDT消息
                timeSlot.setIsNotified(true);
                timeSlot.setHlcTimestamp(newHlc);
                
                // 生成CRDT消息
                try {
                    // 创建消息数据
                    String messageData = objectMapper.writeValueAsString(timeSlot);
                    
                    // 创建同步消息
                    SyncMessage syncMessage = new SyncMessage();
                    syncMessage.setUserId(user.getId());
                    syncMessage.setEntityType("TimeSlot");
                    syncMessage.setCrdtKey(timeSlot.getCrdtKey());
                    syncMessage.setMessageData(messageData);
                    syncMessage.setHlcTimestamp(newHlc);
                    syncMessage.setOriginDeviceId("server-reminder");
                    syncMessage.setCreatedAt(new Timestamp(System.currentTimeMillis()));
                    
                    // 存储消息（由于服务器是纯中继，不再处理消息内容）
                    syncMessageMapper.insert(syncMessage);
                    
                    log.info("已为TimeSlot CRDTKey {} 的通知状态更新生成并存储CRDT消息。", timeSlot.getCrdtKey());
                } catch (Exception e) {
                    log.error("创建TimeSlot通知CRDT消息时出错: {}", e.getMessage(), e);
                }
            } catch (Exception e) {
                log.error("更新TimeSlot CRDTKey {} 的通知状态或生成CRDT消息时出错: {}", timeSlot.getCrdtKey(), e.getMessage(), e);
            }
        } else {
            log.error("未能为TimeSlot CRDTKey: {} 发送微信提醒", timeSlot.getCrdtKey());
        }
    }
    
    /**
     * 获取任务标题
     */
    private String getTaskTitle(TimeSlot timeSlot) {
        if (timeSlot.getScheduleCrdtKey() == null || timeSlot.getScheduleType() == null) return timeSlot.getHead(); // 回退到head字段
        
        if (ScheduleType.ORDINARY.equals(timeSlot.getScheduleType())) {
            OrdinarySchedule schedule = ordinaryScheduleMapper.getByCrdtKey(timeSlot.getScheduleCrdtKey());
            return (schedule != null && !schedule.getIsDeleted()) ? schedule.getTitle() : timeSlot.getHead();
        } else if (ScheduleType.COURSE.equals(timeSlot.getScheduleType())) {
            Course course = courseMapper.getByCrdtKey(timeSlot.getScheduleCrdtKey());
            return (course != null && !course.getIsDeleted()) ? course.getCourseName() : timeSlot.getHead();
        }
        return timeSlot.getHead(); // 回退
    }
    
    /**
     * 获取任务地点
     */
    private String getTaskLocation(TimeSlot timeSlot) {
        if (timeSlot.getScheduleCrdtKey() == null || timeSlot.getScheduleType() == null) return null;

        if (ScheduleType.ORDINARY.equals(timeSlot.getScheduleType())) {
            OrdinarySchedule schedule = ordinaryScheduleMapper.getByCrdtKey(timeSlot.getScheduleCrdtKey());
            return (schedule != null && !schedule.getIsDeleted()) ? schedule.getLocation() : null;
        } else if (ScheduleType.COURSE.equals(timeSlot.getScheduleType())) {
            Course course = courseMapper.getByCrdtKey(timeSlot.getScheduleCrdtKey());
            return (course != null && !course.getIsDeleted()) ? course.getRoom() : null;
        }
        return null;
    }
} 