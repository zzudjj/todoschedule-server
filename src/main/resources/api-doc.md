# TodoSchedule API 文档

## CRDT同步接口规范

为保持API一致性，所有CRDT相关接口应遵循以下字段命名规范：

### 实体统一标识规范

所有实体的唯一标识必须使用 `crdtKey`（Java对象字段名）/`crdt_key`（数据库字段名）。

### CRDT消息格式

客户端发送的CRDT消息必须遵循以下格式：

```json
{
  "crdtKey": "实体唯一标识符",
  "timestamp": 1234567890123,
  "otherFields": "实体其他字段..."
}
```

⚠️ 注意：旧版本使用的 `key` 字段已被弃用，所有客户端必须使用 `crdtKey`。

### 数据库字段与Java对象映射关系

| 数据库字段名 | Java对象字段名 | 说明 |
|------------|--------------|------|
| crdt_key   | crdtKey      | 实体在CRDT模型中的唯一键 |
| entity_type| entityType   | 实体类型，如Course、OrdinarySchedule等 |

## 实体类字段命名规范

所有实体类都必须使用统一的命名方式：

1. `Course` - 主键：`crdtKey`
2. `OrdinarySchedule` - 主键：`crdtKey`
3. `TimeSlot` - 主键：`crdtKey`，关联字段：`scheduleCrdtKey`
4. `SyncMessage` - 主键：`id`，关联字段：`crdtKey`(对应数据库中的`crdt_key`列)

## API接口规范

### 1. 上传CRDT消息

```
POST /sync/messages/{entityType}
```

请求体：
```json
[
  {
    "crdtKey": "unique-id-1",
    "timestamp": 1234567890123,
    "otherFields": "data1"
  },
  {
    "crdtKey": "unique-id-2",
    "timestamp": 1234567890124,
    "otherFields": "data2"
  }
]
```

### 2. 下载CRDT消息

```
GET /sync/messages/{entityType}
```

响应体：
```json
[
  {
    "id": 1,
    "userId": 123,
    "entityType": "Course",
    "crdtKey": "unique-id-1",
    "messageData": "{\"crdtKey\":\"unique-id-1\",\"timestamp\":1234567890123,\"otherFields\":\"data1\"}",
    "hlcTimestamp": 1234567890123,
    "originDeviceId": "device-1",
    "createdAt": "2023-05-12T10:15:30Z"
  }
]
```

## 过渡期处理

为了保证系统的兼容性，服务器将在一段时间内同时支持 `key` 和 `crdtKey` 字段，但所有新的客户端开发必须仅使用 `crdtKey`。 