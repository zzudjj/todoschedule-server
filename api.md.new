# TodoSchedule 数据同步模块 API 文档

## 概述

TodoSchedule 的数据同步模块基于 CRDT（冲突无关数据类型）实现，支持多设备间的数据同步。服务器作为纯消息中继，不处理业务逻辑，只负责存储和转发 CRDT 消息。

所有 API 请求都需要进行身份验证，使用 Bearer Token 机制。每个设备都有一个唯一的设备 ID，用于区分不同设备的消息。

## 基础信息

- **基础路径**: `/sync`
- **认证方式**: Bearer Token
- **内容类型**: application/json

## 通用请求头

| 请求头名称 | 描述 | 是否必须 | 示例 |
|------------|------|----------|------|
| Authorization | 认证令牌，使用 Bearer Token 格式 | 是 | `Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...` |
| X-Device-ID | 客户端设备 ID，用于标识请求来源设备 | 是 | `device_12345678` |

## 错误处理

### 通用错误响应

所有 API 请求可能返回以下错误：

| 状态码 | 错误类型 | 错误代码 | 描述 |
|--------|----------|------|------|
| 400 | Bad Request | 4000 | 请求格式错误或缺少必要参数 |
| 400 | Bad Request | 4001 | 设备ID不能为空 |
| 400 | Bad Request | 4002 | 消息格式错误 |
| 400 | Bad Request | 4003 | 消息列表不能为空 |
| 400 | Bad Request | 4004 | 分页参数无效 |
| 400 | Bad Request | 4005 | 实体类型不支持 |
| 401 | Unauthorized | 4010 | 未授权或令牌无效 |
| 401 | Unauthorized | 4011 | 令牌已过期 |
| 403 | Forbidden | 4030 | 没有足够权限执行操作 |
| 403 | Forbidden | 4031 | 设备未注册或不属于当前用户 |
| 429 | Too Many Requests | 4290 | 请求过于频繁，请稍后重试 |
| 500 | Internal Server Error | 5000 | 服务器内部错误 |
| 503 | Service Unavailable | 5030 | 服务器当前无法处理请求，请稍后重试 |

错误响应示例：

```json
{
  "code": 401,
  "message": "无效的令牌或令牌已过期",
  "data": null
}
```

## API 端点

### 1. 设备注册

注册新设备或更新已有设备信息。

- **URL**: `/sync/device/register`
- **方法**: `POST`
- **描述**: 客户端注册其设备 ID 和名称，服务器将返回设备信息，包括其上次同步的 HLC 时间戳。

#### 请求体

```json
{
  "deviceId": "device_12345678",
  "deviceName": "我的iPhone 15"
}
```

| 字段 | 类型 | 描述 | 是否必须 |
|------|------|------|----------|
| deviceId | String | 设备唯一标识符 | 是 |
| deviceName | String | 设备友好名称 | 否 |

#### 响应

- **状态码**: 200 OK
- **内容类型**: application/json
- **响应体**:

```json
{
  "id": "device_12345678",
  "name": "我的iPhone 15",
  "userId": 1,
  "lastSyncTimestamp": 1621234567890,
  "createdAt": "2023-05-17T10:30:45Z",
  "updatedAt": "2023-05-17T10:30:45Z"
}
```

| 字段 | 类型 | 描述 |
|------|------|------|
| id | String | 设备唯一标识符 |
| name | String | 设备友好名称 |
| userId | Integer | 关联用户的 ID |
| lastSyncTimestamp | Long | 最后同步的 HLC 时间戳 |
| createdAt | String | 设备首次注册时间（ISO 8601 格式） |
| updatedAt | String | 设备信息最后更新时间（ISO 8601 格式） |

#### 错误响应

| 状态码 | 错误描述 |
|--------|----------|
| 400 | deviceId 不能为空 |
| 401 | 无效的令牌或令牌已过期 |

### 2. 上传 CRDT 消息

上传特定实体类型的 CRDT 消息列表。

- **URL**: `/sync/messages/{entityType}`
- **方法**: `POST`
- **描述**: 客户端上传特定实体类型的 CRDT 消息列表。

#### 路径参数

| 参数 | 描述 | 是否必须 | 示例 |
|------|------|----------|------|
| entityType | 实体类型 | 是 | `OrdinarySchedule`, `Course`, `TimeSlot` 等 |

#### 请求头

除了标准请求头外，还需要：

| 请求头名称 | 描述 | 是否必须 | 示例 |
|------------|------|----------|------|
| X-Device-ID | 客户端设备ID | 是 | `device_12345678` |

#### 请求体

请求体是一个 JSON 字符串数组，每个字符串都是一个 CRDT 消息。

```json
[
  "{\"crdtKey\":\"sch_12345\",\"timestamp\":1621234567890,\"originDeviceId\":\"device_12345678\",\"isDeleted\":false,\"data\":{\"id\":\"sch_12345\",\"title\":\"复习考试\",\"content\":\"期末考试复习\",\"startTime\":\"2023-06-20T14:00:00Z\",\"endTime\":\"2023-06-20T16:00:00Z\",\"isCompleted\":false}}",
  "{\"crdtKey\":\"sch_67890\",\"timestamp\":1621234569999,\"originDeviceId\":\"device_12345678\",\"isDeleted\":true,\"data\":{\"id\":\"sch_67890\"}}"
]
```

注意：每个消息都是一个 JSON 字符串，内容如下：

| 字段 | 类型 | 描述 | 是否必须 |
|------|------|------|----------|
| crdtKey | String | CRDT 消息唯一标识符 | 是 |
| timestamp | Long | 混合逻辑时钟时间戳 | 是 |
| originDeviceId | String | 消息来源设备 ID | 是 |
| isDeleted | Boolean | 是否为删除操作 | 是 |
| data | Object | 消息数据 | 是 |

#### 响应

- **状态码**: 200 OK
- **内容类型**: application/json
- **响应体**:

```json
{
  "code": 200,
  "message": "消息已接收",
  "data": null
}
```

响应字段说明：

| 字段 | 类型 | 描述 |
|------|------|------|
| code | Integer | 状态码，200表示成功 |
| message | String | 响应消息 |
| data | Object | 数据内容，这里为null |

#### 错误响应

| 状态码 | 错误描述 |
|--------|----------|
| 400 | X-Device-ID 头部信息不能为空 |
| 400 | 消息列表不能为空 |
| 400 | 请求体格式错误，必须为字符串数组（每个元素为JSON字符串） |
| 401 | 无效的令牌或令牌已过期 |

### 3. 下载所有类型的 CRDT 消息

下载自上次同步以来服务器上所有实体类型的 CRDT 消息。

- **URL**: `/sync/messages/all`
- **方法**: `GET`
- **描述**: 客户端下载自上次同步以来服务器上所有实体类型的 CRDT 消息。

#### 查询参数

| 参数 | 类型 | 描述 | 是否必须 | 示例 |
|------|------|------|----------|------|
| since | Long | HLC 时间戳，表示从哪个时间点之后开始获取消息。如果未提供，则从设备上次记录的 HLC 或 0 开始。 | 否 | 1621234567890 |

#### 响应

- **状态码**: 200 OK
- **内容类型**: application/json
- **响应体**:

```json
[
  {
    "id": 12345,
    "userId": 1,
    "entityType": "OrdinarySchedule",
    "crdtKey": "sch_12345",
    "messageData": "{\"id\":\"sch_12345\",\"title\":\"复习考试\",\"content\":\"期末考试复习\",\"startTime\":\"2023-06-20T14:00:00Z\",\"endTime\":\"2023-06-20T16:00:00Z\",\"isCompleted\":false}",
    "hlcTimestamp": 1621234567890,
    "originDeviceId": "device_abcdef",
    "createdAt": "2023-05-17T10:35:45Z"
  },
  {
    "id": 67890,
    "userId": 1,
    "entityType": "Course",
    "crdtKey": "course_67890",
    "messageData": "{\"id\":\"course_67890\",\"name\":\"高等数学\",\"lecturer\":\"张教授\",\"location\":\"教学楼A-301\"}",
    "hlcTimestamp": 1621234569999,
    "originDeviceId": "device_abcdef",
    "createdAt": "2023-05-17T10:36:10Z"
  }
]
```

响应字段说明：

| 字段 | 类型 | 描述 |
|------|------|------|
| id | Long | 消息 ID |
| userId | Integer | 用户 ID |
| entityType | String | 实体类型 |
| crdtKey | String | CRDT 消息的唯一标识符 |
| messageData | String | CRDT 消息内容（JSON 字符串） |
| hlcTimestamp | Long | 混合逻辑时钟时间戳 |
| originDeviceId | String | 发送消息的原始设备 ID |
| createdAt | String | 消息创建时间（ISO 8601 格式） |

#### 错误响应

| 状态码 | 错误描述 |
|--------|----------|
| 400 | X-Device-ID 头部信息不能为空 |
| 401 | 无效的令牌或令牌已过期 |

### 4. 下载所有类型的 CRDT 消息（排除本设备发出的消息）

下载自上次同步以来服务器上所有实体类型的 CRDT 消息，排除来自当前设备的消息。

- **URL**: `/sync/messages/all/exclude-origin`
- **方法**: `GET`
- **描述**: 客户端下载自上次同步以来服务器上所有实体类型的 CRDT 消息，排除来自当前设备的消息。

#### 查询参数

| 参数 | 类型 | 描述 | 是否必须 | 示例 |
|------|------|------|----------|------|
| since | Long | HLC 时间戳，表示从哪个时间点之后开始获取消息。如果未提供，则从设备上次记录的 HLC 或 0 开始。 | 否 | 1621234567890 |

#### 响应

与 "下载所有类型的 CRDT 消息" 接口相同，但不包含来自当前设备的消息。

#### 错误响应

| 状态码 | 错误描述 |
|--------|----------|
| 400 | X-Device-ID 头部信息不能为空 |
| 401 | 无效的令牌或令牌已过期 |

### 5. 下载特定实体类型的 CRDT 消息

下载自上次同步以来服务器上特定实体类型的 CRDT 消息。

- **URL**: `/sync/messages/{entityType}`
- **方法**: `GET`
- **描述**: 客户端下载自上次同步以来服务器上特定实体类型的 CRDT 消息。

#### 路径参数

| 参数 | 描述 | 是否必须 | 示例 |
|------|------|----------|------|
| entityType | 实体类型 | 是 | `OrdinarySchedule`, `Course`, `TimeSlot` 等 |

#### 查询参数

| 参数 | 类型 | 描述 | 是否必须 | 示例 |
|------|------|------|----------|------|
| since | Long | HLC 时间戳，表示从哪个时间点之后开始获取消息。如果未提供，则从设备上次记录的 HLC 或 0 开始。 | 否 | 1621234567890 |

#### 响应

与 "下载所有类型的 CRDT 消息" 接口相同，但只返回指定实体类型的消息。

#### 错误响应

| 状态码 | 错误描述 |
|--------|----------|
| 400 | X-Device-ID 头部信息不能为空 |
| 401 | 无效的令牌或令牌已过期 |

### 6. 下载特定实体类型的 CRDT 消息（排除本设备发出的消息）

下载自上次同步以来服务器上特定实体类型的 CRDT 消息，排除来自当前设备的消息。

- **URL**: `/sync/messages/{entityType}/exclude-origin`
- **方法**: `GET`
- **描述**: 客户端下载自上次同步以来服务器上特定实体类型的 CRDT 消息，排除来自当前设备的消息。

#### 路径参数

| 参数 | 描述 | 是否必须 | 示例 |
|------|------|----------|------|
| entityType | 实体类型 | 是 | `OrdinarySchedule`, `Course`, `TimeSlot` 等 |

#### 查询参数

| 参数 | 类型 | 描述 | 是否必须 | 示例 |
|------|------|------|----------|------|
| since | Long | HLC 时间戳，表示从哪个时间点之后开始获取消息。如果未提供，则从设备上次记录的 HLC 或 0 开始。 | 否 | 1621234567890 |

#### 响应

与 "下载所有类型的 CRDT 消息" 接口相同，但只返回指定实体类型的消息，并且不包含来自当前设备的消息。

#### 错误响应

| 状态码 | 错误描述 |
|--------|----------|
| 400 | X-Device-ID 头部信息不能为空 |
| 401 | 无效的令牌或令牌已过期 |

### 7. 获取设备列表

获取当前用户的所有已注册设备。

- **URL**: `/sync/devices`
- **方法**: `GET`
- **描述**: 获取当前用户的所有已注册设备。

#### 响应

- **状态码**: 200 OK
- **内容类型**: application/json
- **响应体**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": "device_12345678",
      "name": "我的iPhone 15",
      "userId": 1,
      "lastSyncTimestamp": 1621234567890,
      "createdAt": "2023-05-17T10:30:45Z",
      "updatedAt": "2023-05-17T10:30:45Z"
    },
    {
      "id": "device_87654321",
      "name": "我的iPad Pro",
      "userId": 1,
      "lastSyncTimestamp": 1621234569999,
      "createdAt": "2023-05-18T08:15:30Z",
      "updatedAt": "2023-05-18T08:15:30Z"
    }
  ]
}
```

#### 错误响应

| 状态码 | 错误描述 |
|--------|----------|
| 401 | 无效的令牌或令牌已过期 |

### 8. 批量提交CRDT消息

批量提交各种类型的CRDT消息。

- **URL**: `/sync/messages`
- **方法**: `POST`
- **描述**: 批量提交各种类型的CRDT消息。

#### 请求体

请求体是一个 `SyncMessage` 对象数组。

```json
[
  {
    "id": 12345,
    "userId": 1,
    "entityType": "OrdinarySchedule",
    "crdtKey": "sch_12345",
    "messageData": "{\"id\":\"sch_12345\",\"title\":\"复习考试\",\"content\":\"期末考试复习\",\"startTime\":\"2023-06-20T14:00:00Z\",\"endTime\":\"2023-06-20T16:00:00Z\",\"isCompleted\":false}",
    "hlcTimestamp": 1621234567890,
    "originDeviceId": "device_12345678"
  },
  {
    "id": 67890,
    "userId": 1,
    "entityType": "Course",
    "crdtKey": "course_67890",
    "messageData": "{\"id\":\"course_67890\",\"name\":\"高等数学\",\"lecturer\":\"张教授\",\"location\":\"教学楼A-301\"}",
    "hlcTimestamp": 1621234569999,
    "originDeviceId": "device_12345678"
  }
]
```

#### 响应

- **状态码**: 200 OK
- **内容类型**: application/json
- **响应体**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": ""
}
```

#### 错误响应

| 状态码 | 错误描述 |
|--------|----------|
| 401 | 无效的令牌或令牌已过期 |

## 数据模型

### 设备（Device）

```json
{
  "id": "device_12345678",
  "name": "我的iPhone 15",
  "userId": 1,
  "lastSyncTimestamp": 1621234567890,
  "createdAt": "2023-05-17T10:30:45Z",
  "updatedAt": "2023-05-17T10:30:45Z"
}
```

### 同步消息（SyncMessage）

```json
{
  "id": 12345,
  "userId": 1,
  "entityType": "OrdinarySchedule",
  "crdtKey": "sch_12345",
  "messageData": "{\"id\":\"sch_12345\",\"title\":\"复习考试\",\"content\":\"期末考试复习\",\"startTime\":\"2023-06-20T14:00:00Z\",\"endTime\":\"2023-06-20T16:00:00Z\",\"isCompleted\":false}",
  "hlcTimestamp": 1621234567890,
  "originDeviceId": "device_abcdef",
  "createdAt": "2023-05-17T10:35:45Z"
}
```

### CRDT 消息内容结构

```json
{
  "crdtKey": "sch_12345",
  "hlc": {
    "wallClockTime": 1621234567890,
    "logicalCounter": 42,
    "nodeId": "device_12345678"
  },
  "originDeviceId": "device_12345678",
  "isDeleted": false,
  "operationType": "UPDATE",
  "data": {
    "id": "sch_12345",
    "title": "复习考试",
    "content": "期末考试复习",
    "startTime": "2023-06-20T14:00:00Z",
    "endTime": "2023-06-20T16:00:00Z",
    "isCompleted": false
  }
}
```

### 混合逻辑时钟 (HLC) 结构

```json
{
  "wallClockTime": 1621234567890,
  "logicalCounter": 42,
  "nodeId": "device_12345678"
}
```

| 字段 | 类型 | 描述 |
|------|------|------|
| wallClockTime | Long | 物理墙钟时间（毫秒） |
| logicalCounter | Integer | 逻辑计数器，用于区分同一物理时间的事件 |
| nodeId | String | 节点ID，通常是设备ID |

### 10. 获取服务器 HLC 时间

获取服务器当前的混合逻辑时钟时间，用于客户端与服务器时间同步。

- **URL**: `/sync/server/time`
- **方法**: `GET`
- **描述**: 获取服务器当前的混合逻辑时钟时间。

#### 响应

- **状态码**: 200 OK
- **内容类型**: application/json
- **响应体**:

```json
{
  "wallClockTime": 1621234567890,
  "logicalCounter": 42,
  "nodeId": "server_1"
}
```

| 字段 | 类型 | 描述 |
|------|------|------|
| wallClockTime | Long | 物理墙钟时间（毫秒） |
| logicalCounter | Integer | 逻辑计数器 |
| nodeId | String | 服务器节点 ID |

## 实体类型列表

系统支持以下实体类型的数据同步：

1. `OrdinarySchedule` - 普通日程
2. `Course` - 课程
3. `Table` - 课表
4. `TimeSlot` - 时间段
5. `TableTimeConfig` - 课表时间配置
6. `GlobalTableSetting` - 全局表设置

## 实现注意事项

1. 所有 API 请求必须包含有效的授权令牌和设备 ID
2. 消息上传应使用批量方式，减少网络请求次数
3. 客户端应实现断点续传机制，记录最后同步的时间戳
4. 对于大量消息的场景，应考虑分页获取
5. 客户端应处理网络异常情况，实现消息队列和重试机制

## 使用示例

### 设备注册示例

**请求**:

```http
POST /sync/device/register HTTP/1.1
Host: api.todoschedule.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "deviceId": "device_12345678",
  "deviceName": "我的iPhone 15"
}
```

**响应**:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "device_12345678",
  "name": "我的iPhone 15",
  "userId": 1,
  "lastSyncTimestamp": 1621234567890,
  "createdAt": "2023-05-17T10:30:45Z",
  "updatedAt": "2023-05-17T10:30:45Z"
}
```

### 上传 CRDT 消息示例

**请求**:

```http
POST /sync/messages/OrdinarySchedule HTTP/1.1
Host: api.todoschedule.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-Device-ID: device_12345678
Content-Type: application/json

[
  "{\"crdtKey\":\"sch_12345\",\"hlcTimestamp\":1621234567890,\"originDeviceId\":\"device_12345678\",\"isDeleted\":false,\"operationType\":\"UPDATE\",\"data\":{\"id\":\"sch_12345\",\"title\":\"复习考试\",\"content\":\"期末考试复习\",\"startTime\":\"2023-06-20T14:00:00Z\",\"endTime\":\"2023-06-20T16:00:00Z\",\"isCompleted\":false}}",
  "{\"crdtKey\":\"sch_67890\",\"hlcTimestamp\":1621234569999,\"originDeviceId\":\"device_12345678\",\"isDeleted\":true,\"operationType\":\"DELETE\",\"data\":{\"id\":\"sch_67890\"}}"
]
```

**响应**:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "code": 200,
  "message": "消息已接收",
  "data": null
}
```

### 下载 CRDT 消息示例

**请求**:

```http
GET /sync/messages/all?since=1621234567890 HTTP/1.1
Host: api.todoschedule.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-Device-ID: device_12345678
```

**响应**:

```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": 12345,
    "userId": 1,
    "entityType": "OrdinarySchedule",
    "crdtKey": "sch_12345",
    "messageData": "{\"id\":\"sch_12345\",\"title\":\"复习考试\",\"content\":\"期末考试复习\",\"startTime\":\"2023-06-20T14:00:00Z\",\"endTime\":\"2023-06-20T16:00:00Z\",\"isCompleted\":false}",
    "hlcTimestamp": 1621234567890,
    "originDeviceId": "device_abcdef",
    "createdAt": "2023-05-17T10:35:45Z"
  }
]
```
