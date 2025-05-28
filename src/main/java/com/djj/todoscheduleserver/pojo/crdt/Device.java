package com.djj.todoscheduleserver.pojo.crdt;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class Device {
    private String id;                 // 设备唯一标识符（主键）
    private Integer userId;
    private String name;               // 设备名称
    private Long lastSyncHlcTimestamp; // 此设备最后成功同步到的HLC时间戳
    private Timestamp createdAt;
    private Timestamp updatedAt;
} 