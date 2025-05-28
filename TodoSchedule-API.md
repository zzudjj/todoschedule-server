# TodoSchedule 服务器端 API 接口文档

## 概述

本文档定义了 TodoSchedule 应用与服务器端交互所需的全部 API 接口。这些接口支持用户管理、课表同步、课程信息、日程管理等功能，并支持微信公众号绑定与通知。

## 基础信息

- **基础URL**: `https://api.todoschedule.com/v1`
- **请求格式**: 所有请求使用 JSON 格式提交数据 (`Content-Type: application/json`)
- **响应格式**: 所有响应以 JSON 格式返回 (`Content-Type: application/json`)
- **认证方式**: 除注册和登录接口外，所有请求需在请求头中携带令牌 (`Authorization: Bearer {token}`)

## 通用响应格式

```json
{
  "code": 200,          // 状态码，200表示成功，非200表示错误
  "message": "success", // 提示信息
  "data": {}            // 响应数据，可能是对象、数组或null
}
```

## 错误码说明

| 错误码 | 描述               |
| ------ | ------------------ |
| 200    | 请求成功           |
| 400    | 请求参数错误       |
| 401    | 未认证或认证已过期 |
| 403    | 权限不足           |
| 404    | 资源不存在         |
| 500    | 服务器内部错误     |

---

## 1. 用户模块

### 1.1 用户注册

**请求**
- 方法: POST
- 路径: `/users/register`
- 参数:

```json
{
  "username": "user123",
  "password": "password123",
  "phone_number": "13812345678",
  "email": "example@example.com"
}
```

**响应**

```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "id": 1,
    "username": "user123",
    "token": "jwt-token-value",
    "created_at": "2023-05-15T10:00:00Z"
  }
}
```

### 1.2 用户登录

**请求**
- 方法: POST
- 路径: `/users/login`
- 参数:

```json
{
  "username": "user123",
  "password": "password123"
}
```

**响应**

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "id": 1,
    "username": "user123",
    "token": "jwt-token-value",
    "last_open": "2023-05-15T10:00:00Z"
  }
}
```

### 1.3 获取用户信息

**请求**
- 方法: GET
- 路径: `/users/profile`
- 头部: Authorization: Bearer {token}

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "user123",
    "phone_number": "13812345678",
    "email": "example@example.com",
    "avatar": "https://example.com/avatar.jpg",
    "created_at": "2023-05-15T10:00:00Z",
    "last_open": "2023-05-15T12:30:00Z"
  }
}
```

### 1.4 更新用户信息

**请求**
- 方法: PUT
- 路径: `/users/profile`
- 头部: Authorization: Bearer {token}
- 参数:

```json
{
  "username": "newname",
  "phone_number": "13812345678",
  "email": "newemail@example.com",
  "avatar": "base64-encoded-image-data"
}
```

**响应**

```json
{
  "code": 200,
  "message": "用户信息更新成功",
  "data": {
    "id": 1,
    "username": "newname",
    "phone_number": "13812345678",
    "email": "newemail@example.com",
    "avatar": "https://example.com/avatar.jpg",
    "updated_at": "2023-05-15T14:00:00Z"
  }
}
```

### 1.5 修改密码

**请求**
- 方法: PUT
- 路径: `/users/password`
- 头部: Authorization: Bearer {token}
- 参数:

```json
{
  "old_password": "oldpassword",
  "new_password": "newpassword"
}
```

**响应**

```json
{
  "code": 200,
  "message": "密码修改成功",
  "data": null
}
```

# TodoSchedule 数据同步模块 API 接口文档

## 概述

本文档详细定义了 TodoSchedule 应用数据同步模块的全部 API 接口，用于在移动端应用与服务器之间同步课表、课程、日程等数据。

## 基础信息

- **基础URL**: `https://api.todoschedule.com/v1/sync`
- **请求格式**: 所有请求使用 JSON 格式提交数据 (`Content-Type: application/json`)
- **响应格式**: 所有响应以 JSON 格式返回 (`Content-Type: application/json`)
- **认证方式**: 所有请求需在请求头中携带令牌 (`Authorization: Bearer {token}`)

## 通用响应格式

```json
{
  "code": 200,          // 状态码，200表示成功，非200表示错误
  "message": "success", // 提示信息
  "data": {}            // 响应数据，可能是对象、数组或null
}
```

## 错误码说明

| 错误码 | 描述               |
| ------ | ------------------ |
| 200    | 请求成功           |
| 400    | 请求参数错误       |
| 401    | 未认证或认证已过期 |
| 403    | 权限不足           |
| 404    | 资源不存在         |
| 409    | 数据冲突           |
| 429    | 请求过于频繁       |
| 500    | 服务器内部错误     |

---

## 1. 全量数据上传接口

用于客户端首次使用或重新初始化时，将本地全部数据上传到服务器。

**请求**
- 方法: POST
- 路径: `/upload`
- 头部: Authorization: Bearer {token}
- 参数:

```json
{
  "tables": [
    {
      "id": 1,
      "table_name": "2023春季学期",
      "background": "#FF5733",
      "list_position": 0,
      "terms": "2023春",
      "start_date": "2023-02-20",
      "total_weeks": 20
    }
  ],
  "courses": [
    {
      "id": 1,
      "table_id": 1,
      "course_name": "高等数学",
      "color": "#FF4081",
      "room": "教学楼A-101",
      "teacher": "张教授",
      "credit": 4.0,
      "course_code": "MATH101",
      "syllabus_link": "https://example.com/syllabus/math101.pdf"
    }
  ],
  "course_nodes": [
    {
      "id": 1,
      "course_id": 1,
      "course_node_name": "高数周一课",
      "color": "#FF4081",
      "room": "教学楼A-101",
      "teacher": "张教授",
      "start_node": 1,
      "step": 2,
      "day": 1,
      "start_week": 1,
      "end_week": 16,
      "week_type": 0
    }
  ],
  "ordinary_schedules": [
    {
      "id": 1,
      "title": "小组会议",
      "description": "讨论项目进度",
      "location": "图书馆研讨室",
      "category": "会议",
      "color": "#4CAF50",
      "is_all_day": false,
      "status": "TODO"
    }
  ],
  "time_slots": [
    {
      "id": 1,
      "start_time": 1684151400000,
      "end_time": 1684158600000,
      "schedule_type": "ORDINARY",
      "schedule_id": 1,
      "head": null,
      "priority": 2,
      "is_completed": false,
      "is_repeated": false,
      "repeat_pattern": null,
      "reminder_type": "NOTIFICATION",
      "reminder_offset": 900000
    }
  ],
  "table_time_configs": [
    {
      "id": 1,
      "table_id": 1,
      "name": "默认作息",
      "is_default": true,
      "nodes": [
        {
          "id": 1,
          "name": "第一节",
          "start_time": "08:00:00",
          "end_time": "08:45:00",
          "node": 1
        },
        {
          "id": 2,
          "name": "第二节",
          "start_time": "08:55:00",
          "end_time": "09:40:00",
          "node": 2
        }
      ]
    }
  ],
  "global_settings": {
    "id": 1,
    "default_table_ids": "1",
    "show_weekend": true,
    "course_notification_style": 0,
    "notify_before_minutes": 15,
    "auto_switch_week": true,
    "show_course_time": true
  }
}
```

**响应**

```json
{
  "code": 200,
  "message": "数据同步成功",
  "data": {
    "sync_time": "2023-05-15T16:00:00Z",
    "tables_count": 1,
    "courses_count": 1,
    "course_nodes_count": 1,
    "ordinary_schedules_count": 1,
    "time_slots_count": 1,
    "table_time_configs_count": 1,
    "server_id_mappings": {
      "tables": {"1": 101},
      "courses": {"1": 201},
      "course_nodes": {"1": 301},
      "ordinary_schedules": {"1": 401},
      "time_slots": {"1": 501},
      "table_time_configs": {"1": 601}
    }
  }
}
```

### 字段说明

- `sync_time`: 同步时间戳，客户端应保存此时间用于后续增量同步
- `*_count`: 各类数据的同步数量
- `server_id_mappings`: 服务器端ID映射关系，客户端需要保存这些映射关系用于后续同步

---

## 2. 增量数据同步（上传）接口

用于客户端将本地新增、更新或删除的数据同步到服务器。

**请求**
- 方法: POST
- 路径: `/delta`
- 头部: Authorization: Bearer {token}
- 参数:

```json
{
  "last_sync_time": "2023-05-14T16:00:00Z",
  "client_sync_id": "client-123456",  // 客户端生成的同步标识，用于防重复
  "created": {
    "tables": [],
    "courses": [],
    "course_nodes": [],
    "ordinary_schedules": [
      {
        "id": 2,
        "title": "期末复习",
        "description": "复习高数第1-8章",
        "location": "自习室",
        "category": "学习",
        "color": "#2196F3",
        "is_all_day": false,
        "status": "TODO"
      }
    ],
    "time_slots": [
      {
        "id": 2,
        "start_time": 1684238400000,
        "end_time": 1684245600000,
        "schedule_type": "ORDINARY",
        "schedule_id": 2,
        "head": null,
        "priority": 1,
        "is_completed": false,
        "is_repeated": false,
        "repeat_pattern": null,
        "reminder_type": "NOTIFICATION",
        "reminder_offset": 900000
      }
    ],
    "table_time_configs": [],
    "table_time_config_nodes": []
  },
  "updated": {
    "tables": [],
    "courses": [],
    "course_nodes": [],
    "ordinary_schedules": [
      {
        "id": 401,  // 使用服务器端ID
        "status": "IN_PROGRESS"
      }
    ],
    "time_slots": [
      {
        "id": 501,  // 使用服务器端ID
        "is_completed": true
      }
    ],
    "table_time_configs": [],
    "table_time_config_nodes": [],
    "global_settings": {
      "id": 1,
      "notify_before_minutes": 30
    }
  },
  "deleted": {
    "tables": [],
    "courses": [],
    "course_nodes": [],
    "ordinary_schedules": [],
    "time_slots": [],
    "table_time_configs": [],
    "table_time_config_nodes": []
  }
}
```

**响应**

```json
{
  "code": 200,
  "message": "增量同步成功",
  "data": {
    "sync_time": "2023-05-15T16:05:00Z",
    "client_sync_id": "client-123456",
    "created_count": 2,
    "updated_count": 3,
    "deleted_count": 0,
    "server_id_mappings": {
      "ordinary_schedules": {"2": 402},
      "time_slots": {"2": 502}
    },
    "conflicts": []
  }
}
```

### 字段说明

- `sync_time`: 最新同步时间戳，客户端应更新保存
- `client_sync_id`: 客户端同步标识，原样返回
- `*_count`: 各操作类型的处理数量
- `server_id_mappings`: 新创建数据的服务器端ID映射关系
- `conflicts`: 数据冲突列表，如果为空表示无冲突

---

## 3. 增量数据同步（下载）接口

用于客户端获取服务器端自上次同步后发生变更的数据。

**请求**
- 方法: GET
- 路径: `/delta`
- 头部: Authorization: Bearer {token}
- 参数: 
  - `last_sync_time`: 2023-05-14T16:00:00Z (上次同步时间)
  - `client_sync_id`: client-123456 (可选，客户端同步标识)

**响应**

```json
{
  "code": 200,
  "message": "增量数据获取成功",
  "data": {
    "sync_time": "2023-05-15T16:10:00Z",
    "client_sync_id": "client-123456",
    "created": {
      "tables": [],
      "courses": [],
      "course_nodes": [],
      "ordinary_schedules": [],
      "time_slots": [],
      "table_time_configs": [],
      "table_time_config_nodes": []
    },
    "updated": {
      "tables": [],
      "courses": [
        {
          "id": 201,
          "room": "教学楼A-102",
          "update_time": "2023-05-15T16:08:00Z"
        }
      ],
      "course_nodes": [],
      "ordinary_schedules": [],
      "time_slots": [],
      "table_time_configs": [],
      "table_time_config_nodes": [],
      "global_settings": []
    },
    "deleted": {
      "tables": [],
      "courses": [],
      "course_nodes": [],
      "ordinary_schedules": [],
      "time_slots": [],
      "table_time_configs": [],
      "table_time_config_nodes": []
    }
  }
}
```

### 字段说明

- `sync_time`: 当前服务器同步时间戳，客户端应更新保存
- `client_sync_id`: 客户端同步标识，原样返回
- `created`: 新创建的数据集合
- `updated`: 更新的数据集合，仅包含更新的字段
- `deleted`: 删除的数据ID集合

---

## 4. 全量数据下载接口

用于客户端在首次使用、重置数据或数据不一致时获取服务器端的全量数据。

**请求**
- 方法: GET
- 路径: `/download`
- 头部: Authorization: Bearer {token}

**响应**

```json
{
  "code": 200,
  "message": "数据获取成功",
  "data": {
    "sync_time": "2023-05-15T16:15:00Z",
    "tables": [
      {
        "id": 101,
        "user_id": 1,
        "table_name": "2023春季学期",
        "background": "#FF5733",
        "list_position": 0,
        "terms": "2023春",
        "start_date": "2023-02-20",
        "total_weeks": 20
      }
    ],
    "courses": [
      {
        "id": 201,
        "table_id": 101,
        "course_name": "高等数学",
        "color": "#FF4081",
        "room": "教学楼A-102",
        "teacher": "张教授",
        "credit": 4.0,
        "course_code": "MATH101",
        "syllabus_link": "https://example.com/syllabus/math101.pdf"
      }
    ],
    "course_nodes": [
      {
        "id": 301,
        "course_id": 201,
        "course_node_name": "高数周一课",
        "color": "#FF4081",
        "room": "教学楼A-101",
        "teacher": "张教授",
        "start_node": 1,
        "step": 2,
        "day": 1,
        "start_week": 1,
        "end_week": 16,
        "week_type": 0
      }
    ],
    "ordinary_schedules": [
      {
        "id": 401,
        "user_id": 1,
        "title": "小组会议",
        "description": "讨论项目进度",
        "location": "图书馆研讨室",
        "category": "会议",
        "color": "#4CAF50",
        "is_all_day": false,
        "status": "TODO"
      },
      {
        "id": 402,
        "user_id": 1,
        "title": "期末复习",
        "description": "复习高数第1-8章",
        "location": "自习室",
        "category": "学习",
        "color": "#2196F3",
        "is_all_day": false,
        "status": "IN_PROGRESS"
      }
    ],
    "time_slots": [
      {
        "id": 501,
        "user_id": 1,
        "start_time": 1684151400000,
        "end_time": 1684158600000,
        "schedule_type": "ORDINARY",
        "schedule_id": 401,
        "head": null,
        "priority": 2,
        "is_completed": true,
        "is_repeated": false,
        "repeat_pattern": null,
        "reminder_type": "NOTIFICATION",
        "reminder_offset": 900000
      },
      {
        "id": 502,
        "user_id": 1,
        "start_time": 1684238400000,
        "end_time": 1684245600000,
        "schedule_type": "ORDINARY",
        "schedule_id": 402,
        "head": null,
        "priority": 1,
        "is_completed": false,
        "is_repeated": false,
        "repeat_pattern": null,
        "reminder_type": "NOTIFICATION",
        "reminder_offset": 900000
      }
    ],
    "table_time_configs": [
      {
        "id": 601,
        "table_id": 101,
        "name": "默认作息",
        "is_default": true
      }
    ],
    "table_time_config_nodes": [
      {
        "id": 701,
        "table_time_config_id": 601,
        "name": "第一节",
        "start_time": "08:00:00",
        "end_time": "08:45:00",
        "node": 1
      },
      {
        "id": 702,
        "table_time_config_id": 601,
        "name": "第二节",
        "start_time": "08:55:00",
        "end_time": "09:40:00",
        "node": 2
      }
    ],
    "global_settings": {
      "id": 1,
      "user_id": 1,
      "default_table_ids": "101",
      "show_weekend": true,
      "course_notification_style": 0,
      "notify_before_minutes": 30,
      "auto_switch_week": true,
      "show_course_time": true
    }
  }
}
```

### 字段说明

- `sync_time`: 当前服务器同步时间戳，客户端应保存此时间用于后续增量同步
- 各数据集合使用服务器端ID，客户端需要在本地建立ID映射关系

---

## 5. 同步状态检查接口

用于客户端检查同步状态，确认是否需要执行增量或全量同步。

**请求**
- 方法: GET
- 路径: `/status`
- 头部: Authorization: Bearer {token}
- 参数: 
  - `last_sync_time`: 2023-05-14T16:00:00Z (客户端上次同步时间)
  - `data_hash`: "a1b2c3d4e5f6g7h8i9j0" (可选，客户端数据哈希值)

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "server_time": "2023-05-15T16:20:00Z",
    "last_update_time": "2023-05-15T16:08:00Z",
    "need_sync": true,
    "sync_type": "delta",  // 可能值：delta、full
    "changes_count": 1     // 自上次同步以来的变更数量
  }
}
```

### 字段说明

- `server_time`: 当前服务器时间
- `last_update_time`: 服务器数据最后更新时间
- `need_sync`: 是否需要同步
- `sync_type`: 建议的同步类型，delta(增量)或full(全量)
- `changes_count`: 变更数量，如果为0则表示无需同步

---

## 6. 解决冲突接口

当增量同步出现数据冲突时，用于提交客户端的冲突解决决策。

**请求**
- 方法: POST
- 路径: `/resolve-conflicts`
- 头部: Authorization: Bearer {token}
- 参数:

```json
{
  "client_sync_id": "client-123456",
  "conflict_resolutions": [
    {
      "entity_type": "ordinary_schedules",
      "entity_id": 401,
      "resolution": "client",  // 使用客户端版本
      "client_data": {
        "status": "IN_PROGRESS",
        "title": "小组会议(修改版)"
      }
    },
    {
      "entity_type": "time_slots",
      "entity_id": 501,
      "resolution": "server"   // 使用服务器版本
    }
  ]
}
```

**响应**

```json
{
  "code": 200,
  "message": "冲突解决成功",
  "data": {
    "sync_time": "2023-05-15T16:25:00Z",
    "client_sync_id": "client-123456",
    "resolved_count": 2,
    "remaining_conflicts": []
  }
}
```

### 字段说明

- `sync_time`: 最新同步时间戳，客户端应更新保存
- `client_sync_id`: 客户端同步标识，原样返回
- `resolved_count`: 已解决的冲突数量
- `remaining_conflicts`: 剩余未解决的冲突列表

---

## 7. 同步回滚接口

当同步过程中出现错误或需要恢复到之前的状态时使用。

**请求**
- 方法: POST
- 路径: `/rollback`
- 头部: Authorization: Bearer {token}
- 参数:

```json
{
  "client_sync_id": "client-123456",
  "target_sync_time": "2023-05-14T16:00:00Z"  // 回滚到此时间点
}
```

**响应**

```json
{
  "code": 200,
  "message": "同步回滚成功",
  "data": {
    "sync_time": "2023-05-14T16:00:00Z",
    "client_sync_id": "client-123456",
    "rollback_status": "complete"  // complete或partial
  }
}
```

### 字段说明

- `sync_time`: 回滚后的同步时间戳
- `client_sync_id`: 客户端同步标识，原样返回
- `rollback_status`: 回滚状态，complete(完全回滚)或partial(部分回滚)

---

## 数据结构说明

### 1. 数据实体映射关系

| 客户端数据实体      | 服务器端数据表                |
| ------------------- | ----------------------------- |
| Table               | table                         |
| Course              | course                        |
| CourseNode          | course_node                   |
| OrdinarySchedule    | ordinary_schedule             |
| TimeSlot            | time_slot                     |
| TableTimeConfig     | table_time_config             |
| TableTimeConfigNode | table_time_config_node_detail |
| GlobalSetting       | global_table_setting          |

### 2. 同步策略

1. **客户端策略**:
   - 应在本地维护服务器端ID与本地ID的映射关系
   - 每次同步后保存同步时间戳
   - 处理增量同步时，应根据`created`、`updated`和`deleted`集合更新本地数据
   - 当发生冲突时，可向用户请求决策或采用预定策略处理

2. **服务器端策略**:
   - 使用乐观锁控制策略，通过时间戳检测冲突
   - 保留数据变更历史，支持回滚操作
   - 维护每个用户的最后同步时间

## 注意事项

1. **ID管理**:
   - 客户端应在本地生成临时ID
   - 服务器返回的ID映射关系需要在本地持久化
   - 向服务器发送更新或删除请求时，需使用服务器端ID

2. **时间同步**:
   - 所有时间戳使用UTC时间
   - 客户端应将本地时间转换为UTC时间后传输
   - 服务器端以UTC时间作为同步依据

3. **错误处理**:
   - 当网络错误或服务器错误发生时，客户端应保留同步状态和未同步的更改
   - 对于同步失败的情况，客户端应支持重试机制

4. **性能优化**:
   - 客户端应批量处理同步请求，避免频繁单条数据同步
   - 服务器端采用分页机制返回大量数据

5. **安全措施**:
   - 所有同步请求必须经过授权认证
   - 敏感数据应进行加密处理
   - 服务器应验证数据完整性和合法性