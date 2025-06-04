package com.djj.todoscheduleserver.service.impl;

import com.djj.todoscheduleserver.mapper.DeviceMapper;
import com.djj.todoscheduleserver.mapper.SyncMessageMapper;
import com.djj.todoscheduleserver.pojo.User;
import com.djj.todoscheduleserver.pojo.crdt.Device;
import com.djj.todoscheduleserver.pojo.crdt.SyncMessage;
import com.djj.todoscheduleserver.service.HlcService;
import com.djj.todoscheduleserver.service.SyncService;
import com.djj.todoscheduleserver.service.UserService;
import com.djj.todoscheduleserver.utils.OrdinaryScheduleParserUtil;
import com.djj.todoscheduleserver.utils.TimeSlotParserUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据同步服务实现类
 * 纯中继服务器，负责CRDT消息的存储和转发，不进行消息处理
 */
@Slf4j
@Service
public class SyncServiceImpl implements SyncService {

    @Autowired
    private SyncMessageMapper syncMessageMapper;

    @Autowired
    private DeviceMapper deviceMapper;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private HlcService hlcService;
    
    @Autowired
    private UserService userService;

    @Autowired
    private OrdinaryScheduleParserUtil ordinaryScheduleParserUtil;

    @Autowired
    private TimeSlotParserUtil timeSlotParserUtil;
    
    @Override
    @Transactional
    public void storeClientMessages(User user, String deviceId, String entityType, List<String> messages) {
        log.info("用户 {} 来自设备 {}: 为实体类型 {} 存储 {} 条消息", user.getId(), deviceId, entityType, messages.size());
        
        for (String msgData : messages) {
            try {
                JsonNode rootNode = objectMapper.readTree(msgData);
                long hlc = rootNode.path("hlcTimestamp").asLong(); // 假设Synk消息将'timestamp'作为HLC
                String key = rootNode.path("crdt_key").asText();
                // 尝试兼容旧版本客户端，如果crdtKey不存在，则尝试读取key字段
                if (key == null || key.isEmpty()) {
                    key = rootNode.path("key").asText();
                }

                SyncMessage syncMessage = new SyncMessage();
                syncMessage.setUserId(user.getId());
                syncMessage.setEntityType(entityType);
                syncMessage.setCrdtKey(key);
                syncMessage.setMessageData(msgData);
                syncMessage.setHlcTimestamp(hlc);
                syncMessage.setOriginDeviceId(deviceId);
                syncMessage.setCreatedAt(new Timestamp(System.currentTimeMillis()));
                
                syncMessageMapper.insert(syncMessage);

                ordinaryScheduleParserUtil.processSyncMessage(syncMessage);
                timeSlotParserUtil.processSyncMessage(syncMessage);


            } catch (JsonProcessingException e) {
                log.error("解析来自设备 {} 的CRDT消息失败: {}", deviceId, msgData, e);
                // 可选：存储失败消息记录或通知管理员
            }
        }
    }

    @Override
    public List<SyncMessage> getMessagesForDevice(User user, String deviceId, Long lastSyncHlcTimestamp) {
        log.info("用户 {} 来自设备 {}: 获取自HLC {} 以来的所有消息", user.getId(), deviceId, lastSyncHlcTimestamp);
        Device device = deviceMapper.findById(deviceId);
        if (device == null || !device.getUserId().equals(user.getId())) {
            log.warn("设备 {} 未找到或不属于用户 {}。返回空列表。", deviceId, user.getId());
            return new ArrayList<>();
        }
        
        Long fetchSince = (lastSyncHlcTimestamp != null) ? lastSyncHlcTimestamp : device.getLastSyncHlcTimestamp();
        if (fetchSince == null) fetchSince = 0L; // 初始同步

        List<SyncMessage> messages = syncMessageMapper.getMessagesAfterTimestamp(user.getId(), fetchSince);
        return messages;
    }
    
    @Override
    public List<SyncMessage> getMessagesAfterTimestampExcludingOriginDevice(User user, String deviceId, Long lastSyncHlcTimestamp) {
        log.info("用户 {} 来自设备 {}: 获取自HLC {} 以来的消息(排除原始设备)", user.getId(), deviceId, lastSyncHlcTimestamp);
        Device device = deviceMapper.findById(deviceId);
        if (device == null || !device.getUserId().equals(user.getId())) {
            log.warn("设备 {} 未找到或不属于用户 {}。返回空列表。", deviceId, user.getId());
            return new ArrayList<>();
        }
        
        Long fetchSince = (lastSyncHlcTimestamp != null) ? lastSyncHlcTimestamp : device.getLastSyncHlcTimestamp();
        if (fetchSince == null) fetchSince = 0L; // 初始同步

        return syncMessageMapper.getMessagesAfterTimestampExcludingOriginDevice(user.getId(), deviceId, fetchSince);
    }
    
    @Override
    public List<SyncMessage> getMessagesByEntityTypeForDevice(User user, String deviceId, String entityType, Long lastSyncHlcTimestamp) {
        log.info("用户 {} 来自设备 {}: 获取实体 {} 自HLC {} 以来的消息", user.getId(), deviceId, entityType, lastSyncHlcTimestamp);
        Device device = deviceMapper.findById(deviceId);
        if (device == null || !device.getUserId().equals(user.getId())) {
            log.warn("设备 {} 未找到或不属于用户 {}。返回空列表。", deviceId, user.getId());
            return new ArrayList<>();
        }

        Long fetchSince = (lastSyncHlcTimestamp != null) ? lastSyncHlcTimestamp : device.getLastSyncHlcTimestamp();
        if (fetchSince == null) fetchSince = 0L; // 初始同步

        List<SyncMessage> messages = syncMessageMapper.getMessagesByEntityTypeAfterTimestamp(user.getId(), entityType, fetchSince);
        return messages;
    }
    
    @Override
    public List<SyncMessage> getMessagesByEntityTypeAfterTimestampExcludingOriginDevice(User user, String deviceId, String entityType, Long lastSyncHlcTimestamp) {
        log.info("用户 {} 来自设备 {}: 获取实体 {} 自HLC {} 以来的消息(排除原始设备)", user.getId(), deviceId, entityType, lastSyncHlcTimestamp);
        Device device = deviceMapper.findById(deviceId);
        if (device == null || !device.getUserId().equals(user.getId())) {
            log.warn("设备 {} 未找到或不属于用户 {}。返回空列表。", deviceId, user.getId());
            return new ArrayList<>();
        }

        Long fetchSince = (lastSyncHlcTimestamp != null) ? lastSyncHlcTimestamp : device.getLastSyncHlcTimestamp();
        if (fetchSince == null) fetchSince = 0L; // 初始同步

        return syncMessageMapper.getMessagesByEntityTypeAfterTimestampExcludingOriginDevice(user.getId(), deviceId, entityType, fetchSince);
    }

    @Override
    @Transactional
    public Device registerDevice(Device device, String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("授权头部缺失或格式不正确");
            return null;
        }
        String token = authorization.substring(7); // 去掉"Bearer "前缀
        User user = userService.getUserByToken(token);
        if (user == null) {
            log.warn("注册设备 {} 失败，无效的用户令牌: {}", device.getId(), token);
            throw new RuntimeException("用户未登录");
        }

        device.setUserId(user.getId());
        
        // 检查设备是否已存在
        Device existingDevice = deviceMapper.findById(device.getId());
        if (existingDevice != null) {
            // 更新设备信息
            device.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            deviceMapper.update(device);
            return device;
        } else {
            // 创建新设备
            device.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            device.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            if (device.getLastSyncHlcTimestamp() == null) {
                device.setLastSyncHlcTimestamp(0L);
            }
            deviceMapper.insert(device);
            return device;
        }
    }

    @Override
    public List<Device> getDevicesByToken(String token) {
        User user = userService.getUserByToken(token);
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        return deviceMapper.findByUserId(user.getId());
    }

    @Override
    @Transactional
    public void processMessages(List<SyncMessage> messages, String token) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        User user = userService.getUserByToken(token);
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }

        for (SyncMessage message : messages) {
            message.setUserId(user.getId());
            message.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            
            // 存储消息
            syncMessageMapper.insert(message);
        }
    }

    @Override
    public List<SyncMessage> getMessagesAfterTimestamp(Long timestamp, String deviceId, String token) {
        User user = userService.getUserByToken(token);
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }

        List<SyncMessage> messages = syncMessageMapper.getMessagesAfterTimestamp(user.getId(), timestamp);
        
        // 如果提供了设备ID，更新设备的最后同步时间戳
        if (deviceId != null && !deviceId.isEmpty() && !messages.isEmpty()) {
            Long maxTimestamp = messages.stream()
                    .map(SyncMessage::getHlcTimestamp)
                    .max(Long::compare)
                    .orElse(timestamp);
            
            updateDeviceLastSyncTimestamp(deviceId, maxTimestamp, token);
        }
        
        return messages;
    }

    @Override
    @Transactional
    public void updateDeviceLastSyncTimestamp(String deviceId, Long timestamp, String token) {
        User user = userService.getUserByToken(token);
        if (user == null) {
            log.warn("更新设备 {} 的最后同步时间戳失败，无效的用户令牌: {}", deviceId, token);
            throw new RuntimeException("用户未登录或令牌无效");
        }

        Device device = deviceMapper.findById(deviceId);
        if (device == null || !device.getUserId().equals(user.getId())) {
            log.warn("更新设备 {} 的最后同步时间戳失败，设备不存在或不属于用户 {}", deviceId, user.getId());
            throw new RuntimeException("设备不存在或不属于当前用户");
        }
        
        if (timestamp == null) {
            log.warn("尝试为设备 {} 更新一个空的(null)最后同步时间戳，操作被忽略。", deviceId);
            return;
        }

        log.info("用户 {} 请求更新设备 {} 的最后同步HLC为: {}", user.getId(), deviceId, timestamp);
        deviceMapper.updateLastSyncHlcTimestamp(deviceId, timestamp, new Timestamp(System.currentTimeMillis()));
    }
} 