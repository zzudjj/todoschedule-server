package com.djj.todoscheduleserver.utils;

import com.djj.todoscheduleserver.mapper.OrdinaryScheduleMapper;
import com.djj.todoscheduleserver.pojo.OrdinarySchedule;
import com.djj.todoscheduleserver.pojo.crdt.SyncMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * 用于解析和处理OrdinarySchedule相关的CRDT同步消息的工具类
 */
@Component
public class OrdinaryScheduleParserUtil {

    private final OrdinaryScheduleMapper ordinaryScheduleMapper;
    private final ObjectMapper objectMapper;

    public OrdinaryScheduleParserUtil(OrdinaryScheduleMapper ordinaryScheduleMapper) {
        this.ordinaryScheduleMapper = ordinaryScheduleMapper;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 处理SyncMessage并将其转换为OrdinarySchedule实体并保存到数据库
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
            System.err.println("处理OrdinarySchedule同步消息时出错: " + e.getMessage());
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
        OrdinarySchedule ordinarySchedule = parseOrdinarySchedule(syncMessage);
        
        if (ordinarySchedule != null) {
            // 检查数据库中是否已存在该实体
            OrdinarySchedule existingSchedule = ordinaryScheduleMapper.getByCrdtKey(ordinarySchedule.getCrdtKey());
            
            // 如果存在并且已有的时间戳更新，则不进行操作
            if (existingSchedule != null && existingSchedule.getHlcTimestamp() != null && 
                existingSchedule.getHlcTimestamp() >= ordinarySchedule.getHlcTimestamp()) {
                return true; // 已存在更新的版本，无需更新
            }
            
            // 设置默认值
            if (ordinarySchedule.getIsDeleted() == null) {
                ordinarySchedule.setIsDeleted(false);
            }
            
            // 插入或更新到数据库
            int result = ordinaryScheduleMapper.insertOrUpdate(ordinarySchedule);
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
        int result = ordinaryScheduleMapper.markAsDeleted(
            crdtKey, 
            hlcTimestamp,
            new Timestamp(Instant.now().toEpochMilli())
        );
        
        return result > 0;
    }

    /**
     * 解析同步消息中的JSON数据，转换为OrdinarySchedule对象
     */
    private OrdinarySchedule parseOrdinarySchedule(SyncMessage syncMessage) throws JsonProcessingException {
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

        OrdinarySchedule schedule = new OrdinarySchedule();
        
        // 设置基本属性
        // 重要！使用crdt_key而非id
        schedule.setCrdtKey(dataNode.has("crdt_key") ? dataNode.get("crdt_key").asText() : 
                         (dataNode.has("crdtKey") ? dataNode.get("crdtKey").asText() : syncMessage.getCrdtKey()));
        schedule.setUserId(syncMessage.getUserId());
        schedule.setTitle(dataNode.has("title") ? dataNode.get("title").asText() : null);
        schedule.setDescription(dataNode.has("description") && !dataNode.get("description").isNull() ? 
                              dataNode.get("description").asText() : null);
        schedule.setLocation(dataNode.has("location") && !dataNode.get("location").isNull() ? 
                           dataNode.get("location").asText() : null);
        schedule.setCategory(dataNode.has("category") && !dataNode.get("category").isNull() ? 
                           dataNode.get("category").asText() : null);
        schedule.setColor(dataNode.has("color") && !dataNode.get("color").isNull() ? 
                        dataNode.get("color").asText() : null);
        
        // 解析布尔值
        if (dataNode.has("isAllDay")) {
            String isAllDayStr = dataNode.get("isAllDay").asText();
            schedule.setIsAllDay(Boolean.parseBoolean(isAllDayStr));
        }
        
        schedule.setStatus(dataNode.has("status") ? dataNode.get("status").asText() : null);
        
        // 设置优先级
        if (dataNode.has("priority") && !dataNode.get("priority").isNull()) {
            schedule.setPriority(dataNode.get("priority").asInt());
        }
        
        // 设置完成状态
        if (dataNode.has("completed")) {
            String completedStr = dataNode.get("completed").asText();
            schedule.setCompleted(Boolean.parseBoolean(completedStr));
        } else {
            schedule.setCompleted(false);
        }
        
        // 设置CRDT相关字段
        schedule.setHlcTimestamp(syncMessage.getHlcTimestamp());
        schedule.setIsDeleted(false);
        
        return schedule;
    }
}
