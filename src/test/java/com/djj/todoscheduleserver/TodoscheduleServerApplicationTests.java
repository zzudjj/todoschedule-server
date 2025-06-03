package com.djj.todoscheduleserver;

import com.djj.todoscheduleserver.mapper.OrdinaryScheduleMapper;
import com.djj.todoscheduleserver.mapper.SyncMessageMapper;
import com.djj.todoscheduleserver.mapper.TimeSlotMapper;
import com.djj.todoscheduleserver.pojo.OrdinarySchedule;
import com.djj.todoscheduleserver.pojo.TimeSlot;
import com.djj.todoscheduleserver.pojo.crdt.SyncMessage;
import com.djj.todoscheduleserver.utils.OrdinaryScheduleParserUtil;
import com.djj.todoscheduleserver.utils.TimeSlotParserUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
class TodoscheduleServerApplicationTests {

    @Autowired
    private SyncMessageMapper syncMessageMapper;
    
    @Autowired
    private OrdinaryScheduleParserUtil ordinaryScheduleParserUtil;
    
    @Autowired
    private TimeSlotParserUtil timeSlotParserUtil;
    
    @Autowired
    private OrdinaryScheduleMapper ordinaryScheduleMapper;
    
    @Autowired
    private TimeSlotMapper timeSlotMapper;

    @Test
    void contextLoads() {
    }
    
    /**
     * 插入模拟日程和时间槽数据
     * 日程的开始时间为当前时间的未来2分钟，结束时间为未来5分钟
     */
    @Test
    void insertMockScheduleAndTimeSlot() {
        // 用户ID
        Integer userId = 1;
        
        // 计算开始和结束时间（毫秒时间戳）
        Instant now = Instant.now();
        Long startTimeMillis = now.plus(7, ChronoUnit.MINUTES).toEpochMilli();
        Long endTimeMillis = now.plus(10, ChronoUnit.MINUTES).toEpochMilli();
        
        // 生成唯一的CRDT键
        String scheduleCrdtKey = "ordinary_schedule_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String timeSlotCrdtKey = "time_slot_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        
        // 生成HLC时间戳（使用当前时间毫秒值作为简化的HLC时间戳）
        Long hlcTimestamp = now.toEpochMilli();
        
        // 创建OrdinarySchedule实体
        OrdinarySchedule schedule = new OrdinarySchedule();
        schedule.setCrdtKey(scheduleCrdtKey);
        schedule.setUserId(userId);
        schedule.setTitle("测试日程 - " + startTimeMillis);
        schedule.setDescription("这是一个自动创建的测试日程，开始于未来2分钟");
        schedule.setLocation("测试位置");
        schedule.setCategory("测试");
        schedule.setColor("#FF5733");
        schedule.setIsAllDay(false);
        schedule.setStatus("pending");
        schedule.setStartTime(String.valueOf(startTimeMillis));
        schedule.setEndTime(String.valueOf(endTimeMillis));
        schedule.setPriority(1);
        schedule.setCompleted(false);
        schedule.setHlcTimestamp(hlcTimestamp);
        schedule.setIsDeleted(false);
        
        // 保存日程到数据库
        int scheduleResult = ordinaryScheduleMapper.insertOrUpdate(schedule);
        System.out.println("日程插入结果: " + (scheduleResult > 0 ? "成功" : "失败"));
        System.out.println("日程CRDT键: " + scheduleCrdtKey);
        System.out.println("日程开始时间: " + new Timestamp(startTimeMillis));
        System.out.println("日程结束时间: " + new Timestamp(endTimeMillis));
        
        // 创建TimeSlot实体
        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setCrdtKey(timeSlotCrdtKey);
        timeSlot.setUserId(userId);
        timeSlot.setStartTime(startTimeMillis);
        timeSlot.setEndTime(endTimeMillis);
        timeSlot.setScheduleType("ORDINARY"); // 普通日程类型
        timeSlot.setScheduleCrdtKey(scheduleCrdtKey); // 关联到刚才创建的日程
        timeSlot.setHead("测试时间槽");
        timeSlot.setPriority(1);
        timeSlot.setIsCompleted(false);
        timeSlot.setIsRepeated(false);
        timeSlot.setReminderType("NOTIFICATION");
        timeSlot.setReminderOffset(5 * 60 * 1000L); // 5分钟前提醒
        timeSlot.setIsNotified(false);
        timeSlot.setHlcTimestamp(hlcTimestamp);
        timeSlot.setIsDeleted(false);
        
        // 保存时间槽到数据库
        int timeSlotResult = timeSlotMapper.insertOrUpdate(timeSlot);
        System.out.println("时间槽插入结果: " + (timeSlotResult > 0 ? "成功" : "失败"));
        System.out.println("时间槽CRDT键: " + timeSlotCrdtKey);
        System.out.println("关联的日程CRDT键: " + scheduleCrdtKey);
    }
    
    /**
     * 测试同步消息解析和保存
     * 扫描sync_message表中的所有消息并解析保存到相应的数据库中
     */
    @Test
    void testSyncMessageParsing() {
        // 获取所有同步消息
        List<SyncMessage> allMessages = syncMessageMapper.getAllMessagesForUser(1);
        System.out.println("共加载 " + allMessages.size() + " 条同步消息");
        
        AtomicInteger ordinaryScheduleCount = new AtomicInteger(0);
        AtomicInteger timeSlotCount = new AtomicInteger(0);
        AtomicInteger otherCount = new AtomicInteger(0);
        
        // 处理每条消息
        allMessages.forEach(message -> {
            String entityType = message.getEntityType();
            boolean processed = false;
            
            // 根据实体类型分发到相应的解析器
            if ("OrdinarySchedule".equals(entityType)) {
                processed = ordinaryScheduleParserUtil.processSyncMessage(message);
                if (processed) {
                    ordinaryScheduleCount.incrementAndGet();
                }
            } else if ("TimeSlot".equals(entityType)) {
                processed = timeSlotParserUtil.processSyncMessage(message);
                if (processed) {
                    timeSlotCount.incrementAndGet();
                }
            } else {
                otherCount.incrementAndGet();
            }
            
            // 打印处理结果
            System.out.println("处理消息 ID: " + message.getId() + 
                             ", 类型: " + entityType + 
                             ", 结果: " + (processed ? "成功" : "失败"));
        });
        
        // 汇总结果
        System.out.println("===== 处理结果汇总 =====");
        System.out.println("处理普通日程消息: " + ordinaryScheduleCount.get() + " 条");
        System.out.println("处理时间槽消息: " + timeSlotCount.get() + " 条");
        System.out.println("其他类型消息: " + otherCount.get() + " 条");
        System.out.println("总计: " + allMessages.size() + " 条");
    }
}
