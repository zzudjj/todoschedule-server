package com.djj.todoscheduleserver.pojo.crdt;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class SyncMessage {
    private Long id;              // 数据库主键id，自增
    private Integer userId;       // 用户ID
    private String entityType;    // 实体类型，例如：Course、OrdinarySchedule
    private String crdtKey;       // 实体在CRDT模型中的唯一键，数据库中是crdt_key
    private String messageData;   // JSON格式的CRDT消息数据
    private Long hlcTimestamp;    // 混合逻辑时钟时间戳
    private String originDeviceId;// 消息来源设备ID
    private Timestamp createdAt;  // 记录创建时间
} 