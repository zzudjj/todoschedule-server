package com.djj.todoscheduleserver.service;

import com.djj.todoscheduleserver.pojo.User;
import com.djj.todoscheduleserver.pojo.crdt.Device;
import com.djj.todoscheduleserver.pojo.crdt.SyncMessage;
// import com.djj.todoscheduleserver.pojo.sync.SyncRequest; // 如果直接使用CRDT消息，则可能不需要
// import com.djj.todoscheduleserver.pojo.sync.SyncResponse; // CRDT消息的响应将有所不同

import java.util.List;

/**
 * 同步服务接口
 * 负责处理客户端与服务器之间的数据同步和CRDT消息的存储及转发
 */
public interface SyncService {

    /**
     * 存储从客户端设备接收到的CRDT消息
     * @param user 经过身份验证的用户
     * @param deviceId 发送消息的设备ID
     * @param entityType 这些消息所属的实体类型（例如："OrdinarySchedule", "Course"）
     * @param messages 来自客户端的CRDT消息列表（JSON字符串）
     */
    void storeClientMessages(User user, String deviceId, String entityType, List<String> messages);

    /**
     * 检索客户端设备的CRDT消息，这些消息比该设备最后已知的HLC时间戳要新
     * @param user 经过身份验证的用户
     * @param deviceId 请求消息的设备ID
     * @param lastSyncHlcTimestamp 客户端最后成功处理的消息的HLC时间戳
     * @return CRDT消息列表
     */
    List<SyncMessage> getMessagesForDevice(User user, String deviceId, Long lastSyncHlcTimestamp);
    
    /**
     * 检索客户端设备的CRDT消息，这些消息比该设备最后已知的HLC时间戳要新，并排除来自该设备的消息
     * @param user 经过身份验证的用户
     * @param deviceId 请求消息的设备ID
     * @param lastSyncHlcTimestamp 客户端最后成功处理的消息的HLC时间戳
     * @return CRDT消息列表
     */
    List<SyncMessage> getMessagesAfterTimestampExcludingOriginDevice(User user, String deviceId, Long lastSyncHlcTimestamp);
    
    /**
     * 检索特定实体类型的CRDT消息，这些消息比给定的HLC时间戳新
     * @param user 经过身份验证的用户
     * @param deviceId 设备ID（用于更新其last_sync_hlc_timestamp）
     * @param entityType 要检索消息的实体类型
     * @param lastSyncHlcTimestamp HLC时间戳
     * @return CRDT消息列表
     */
    List<SyncMessage> getMessagesByEntityTypeForDevice(User user, String deviceId, String entityType, Long lastSyncHlcTimestamp);

    /**
     * 检索特定实体类型的CRDT消息，这些消息比给定的HLC时间戳新，并排除来自该设备的消息
     * @param user 经过身份验证的用户
     * @param deviceId 设备ID（用于更新其last_sync_hlc_timestamp）
     * @param entityType 要检索消息的实体类型
     * @param lastSyncHlcTimestamp HLC时间戳
     * @return CRDT消息列表
     */
    List<SyncMessage> getMessagesByEntityTypeAfterTimestampExcludingOriginDevice(User user, String deviceId, String entityType, Long lastSyncHlcTimestamp);
    
    /**
     * 注册或更新设备
     * @param device 设备信息
     * @param token 用户token
     * @return 注册或更新后的Device对象
     */
    Device registerDevice(Device device, String token);
    
    /**
     * 根据用户token获取设备列表
     * @param token 用户token
     * @return 设备列表
     */
    List<Device> getDevicesByToken(String token);
    
    /**
     * 处理同步消息列表
     * @param messages 同步消息列表
     * @param token 用户token
     */
    void processMessages(List<SyncMessage> messages, String token);
    
    /**
     * 获取指定时间戳之后的消息
     * @param timestamp 时间戳
     * @param deviceId 设备ID
     * @param token 用户token
     * @return 消息列表
     */
    List<SyncMessage> getMessagesAfterTimestamp(Long timestamp, String deviceId, String token);
    
    /**
     * 更新设备最后同步时间戳
     * @param deviceId 设备ID
     * @param timestamp 时间戳
     * @param token 用户token
     */
    void updateDeviceLastSyncTimestamp(String deviceId, Long timestamp, String token);
} 