package com.djj.todoscheduleserver.mapper;

import com.djj.todoscheduleserver.pojo.crdt.SyncMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SyncMessageMapper {
    /**
     * 插入新的同步消息
     */
    int insert(SyncMessage syncMessage);

    /**
     * 获取指定时间戳之后的所有消息
     */
    List<SyncMessage> getMessagesAfterTimestamp(
            @Param("userId") Integer userId,
            @Param("hlcTimestamp") Long hlcTimestamp
    );

    /**
     * 获取指定时间戳之后的所有消息，排除来自特定设备的消息
     */
    List<SyncMessage> getMessagesAfterTimestampExcludingOriginDevice(
            @Param("userId") Integer userId,
            @Param("deviceId") String deviceId,
            @Param("hlcTimestamp") Long hlcTimestamp
    );

    /**
     * 获取指定实体类型和时间戳之后的消息
     */
    List<SyncMessage> getMessagesByEntityTypeAfterTimestamp(
            @Param("userId") Integer userId,
            @Param("entityType") String entityType,
            @Param("hlcTimestamp") Long hlcTimestamp
    );
    
    /**
     * 获取指定实体类型和时间戳之后的消息，排除来自特定设备的消息
     */
    List<SyncMessage> getMessagesByEntityTypeAfterTimestampExcludingOriginDevice(
            @Param("userId") Integer userId,
            @Param("deviceId") String deviceId,
            @Param("entityType") String entityType,
            @Param("hlcTimestamp") Long hlcTimestamp
    );
    
    /**
     * 获取用户的所有消息（用于初始同步）
     */
    List<SyncMessage> getAllMessagesForUser(@Param("userId") Integer userId);
    
    /**
     * 获取指定实体的所有消息
     */
    List<SyncMessage> getMessagesByEntityKey(
            @Param("userId") Integer userId,
            @Param("entityType") String entityType,
            @Param("entityKey") String entityKey
    );
} 