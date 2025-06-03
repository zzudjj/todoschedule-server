package com.djj.todoscheduleserver.utils;

import com.djj.todoscheduleserver.mapper.TimeSlotMapper;
import com.djj.todoscheduleserver.pojo.TimeSlot;
import com.djj.todoscheduleserver.pojo.crdt.SyncMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * 用于解析和处理TimeSlot相关的CRDT同步消息的工具类
 */
@Component
public class TimeSlotParserUtil {

    private final TimeSlotMapper timeSlotMapper;
    private final ObjectMapper objectMapper;

    public TimeSlotParserUtil(TimeSlotMapper timeSlotMapper) {
        this.timeSlotMapper = timeSlotMapper;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 处理SyncMessage并将其转换为TimeSlot实体并保存到数据库
     * 
     * @param syncMessage 同步消息对象
     * @return 是否成功处理
     */
    public boolean processSyncMessage(SyncMessage syncMessage) {
        try {
            String operationType = getOperationType(syncMessage);
            
            if ("DELETE".equals(operationType)) {
                return handleDeleteOperation(syncMessage);
            } else {
                return handleAddOrUpdateOperation(syncMessage);
            }
        } catch (Exception e) {
            // 记录异常信息
            System.err.println("处理TimeSlot同步消息时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 从同步消息中获取操作类型
     */
    private String getOperationType(SyncMessage syncMessage) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(syncMessage.getMessageData());
        return rootNode.has("operationType") ? 
               rootNode.get("operationType").asText() : 
               "";
    }

    /**
     * 处理添加或更新操作
     */
    private boolean handleAddOrUpdateOperation(SyncMessage syncMessage) throws JsonProcessingException {
        TimeSlot timeSlot = parseTimeSlot(syncMessage);
        
        if (timeSlot != null) {
            // 检查数据库中是否已存在该实体
            TimeSlot existingTimeSlot = timeSlotMapper.getByCrdtKey(timeSlot.getCrdtKey());
            
            // 如果存在并且已有的时间戳更新，则不进行操作
            if (existingTimeSlot != null && existingTimeSlot.getHlcTimestamp() != null && 
                existingTimeSlot.getHlcTimestamp() >= timeSlot.getHlcTimestamp()) {
                return true; // 已存在更新的版本，无需更新
            }
            
            // 设置默认值
            if (timeSlot.getIsDeleted() == null) {
                timeSlot.setIsDeleted(false);
            }
            if (timeSlot.getIsNotified() == null) {
                timeSlot.setIsNotified(false);
            }
            
            // 插入或更新到数据库
            int result = timeSlotMapper.insertOrUpdate(timeSlot);
            return result > 0;
        }
        
        return false;
    }

    /**
     * 处理删除操作
     */
    private boolean handleDeleteOperation(SyncMessage syncMessage) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(syncMessage.getMessageData());
        String crdtKey = rootNode.get("crdt_key").asText();
        Long hlcTimestamp = syncMessage.getHlcTimestamp();
        
        // 执行软删除
        int result = timeSlotMapper.markAsDeleted(
            crdtKey, 
            hlcTimestamp,
            new Timestamp(Instant.now().toEpochMilli())
        );
        
        return result > 0;
    }

    /**
     * 解析同步消息中的JSON数据，转换为TimeSlot对象
     */
    private TimeSlot parseTimeSlot(SyncMessage syncMessage) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(syncMessage.getMessageData());
        String messageDataStr = rootNode.has("messageData") ? 
                                rootNode.get("messageData").asText() : 
                                syncMessage.getMessageData();
        
        // 解析内嵌的messageData字段，如果有的话
        JsonNode dataNode;
        try {
            dataNode = objectMapper.readTree(messageDataStr);
        } catch (Exception e) {
            // 如果messageData不是有效的JSON，则使用外层的消息
            dataNode = rootNode;
        }

        TimeSlot timeSlot = new TimeSlot();
        
        // 设置基本属性
        // 重要！使用crdt_key而非id
        timeSlot.setCrdtKey(dataNode.has("crdt_key") ? dataNode.get("crdt_key").asText() : 
                         (dataNode.has("crdtKey") ? dataNode.get("crdtKey").asText() : syncMessage.getCrdtKey()));
        timeSlot.setUserId(syncMessage.getUserId());
        
        // 设置时间相关字段
        if (dataNode.has("startTime") && !dataNode.get("startTime").isNull()) {
            try {
                timeSlot.setStartTime(Long.parseLong(dataNode.get("startTime").asText()));
            } catch (NumberFormatException e) {
                // 如果无法解析为数字，则设为null
                timeSlot.setStartTime(null);
            }
        } else if (dataNode.has("start_time") && !dataNode.get("start_time").isNull()) {
            try {
                timeSlot.setStartTime(Long.parseLong(dataNode.get("start_time").asText()));
            } catch (NumberFormatException e) {
                timeSlot.setStartTime(null);
            }
        }
        
        if (dataNode.has("endTime") && !dataNode.get("endTime").isNull()) {
            try {
                timeSlot.setEndTime(Long.parseLong(dataNode.get("endTime").asText()));
            } catch (NumberFormatException e) {
                // 如果无法解析为数字，则设为null
                timeSlot.setEndTime(null);
            }
        }
        
        // 设置其他属性
        // 注意使用scheduleType或schedule_type
        timeSlot.setScheduleType(dataNode.has("scheduleType") ? dataNode.get("scheduleType").asText() : 
                               (dataNode.has("schedule_type") ? dataNode.get("schedule_type").asText() : null));
        
        // 重要！始终使用crdt_key而不是id
        // 处理scheduleCrdtKey字段 - 必须是字符串的CRDT键，不能是数字ID
        String scheduleCrdtKey = null;
        
        if (dataNode.has("scheduleCrdtKey") && !dataNode.get("scheduleCrdtKey").isNull()) {
            scheduleCrdtKey = dataNode.get("scheduleCrdtKey").asText();
        } else if (dataNode.has("schedule_crdt_key") && !dataNode.get("schedule_crdt_key").isNull()) {
            scheduleCrdtKey = dataNode.get("schedule_crdt_key").asText();
        } else if (dataNode.has("scheduleId") && !dataNode.get("scheduleId").isNull()) {
            // 这里的scheduleId应该是字符串格式的CRDT键，而不是数字ID
            // 检查是否是纯数字ID，如果是纯数字，则当作为ID处理
            String potentialKey = dataNode.get("scheduleId").asText();
            if (potentialKey.matches("^\\d+$")) {
                // 这是数字ID，需要生成或查找对应的CRDT key
                // 我们使用前缀加ID的方式生成一个有效的CRDT key
                scheduleCrdtKey = "ordinary_schedule_" + potentialKey;
                System.out.println("将数字ID " + potentialKey + " 转换为CRDT key: " + scheduleCrdtKey);
            } else {
                // 这已经是字符串格式的key
                scheduleCrdtKey = potentialKey;
            }
        }
        
        // 设置crdt_key
        timeSlot.setScheduleCrdtKey(scheduleCrdtKey);
        
        timeSlot.setHead(dataNode.has("head") && !dataNode.get("head").isNull() ? 
                        dataNode.get("head").asText() : null);
        
        if (dataNode.has("priority") && !dataNode.get("priority").isNull()) {
            try {
                timeSlot.setPriority(Integer.parseInt(dataNode.get("priority").asText()));
            } catch (NumberFormatException e) {
                timeSlot.setPriority(null);
            }
        }
        
        // 解析布尔值
        if (dataNode.has("isCompleted")) {
            String isCompletedStr = dataNode.get("isCompleted").asText();
            timeSlot.setIsCompleted(Boolean.parseBoolean(isCompletedStr));
        } else {
            timeSlot.setIsCompleted(false);
        }
        
        if (dataNode.has("isRepeated")) {
            String isRepeatedStr = dataNode.get("isRepeated").asText();
            timeSlot.setIsRepeated(Boolean.parseBoolean(isRepeatedStr));
        } else {
            timeSlot.setIsRepeated(false);
        }
        
        timeSlot.setRepeatPattern(dataNode.has("repeatPattern") && !dataNode.get("repeatPattern").isNull() ? 
                                 dataNode.get("repeatPattern").asText() : null);
        timeSlot.setReminderType(dataNode.has("reminderType") ? dataNode.get("reminderType").asText() : "NONE");
        
        if (dataNode.has("reminderOffset") && !dataNode.get("reminderOffset").isNull()) {
            try {
                timeSlot.setReminderOffset(Long.parseLong(dataNode.get("reminderOffset").asText()));
            } catch (NumberFormatException e) {
                timeSlot.setReminderOffset(null);
            }
        }
        
        // 设置通知状态
        timeSlot.setIsNotified(false);
        
        // 设置CRDT相关字段
        timeSlot.setHlcTimestamp(syncMessage.getHlcTimestamp());
        timeSlot.setIsDeleted(false);
        
        return timeSlot;
    }
}
