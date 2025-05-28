# 微信公众号任务提醒系统

基于Spring Boot和MyBatis的微信公众号任务提醒系统，用于在任务开始前通过微信公众号发送提醒消息。

## 项目结构

- **控制器**：处理微信请求
  - `WechatController`: 处理微信服务器验证和消息交互
  - `WechatOAuthController`: 处理微信网页授权

- **服务层**：处理业务逻辑
  - `WechatService`: 处理微信消息和发送模板消息
  - `ReminderService`: 定时检查任务并发送提醒

- **映射层**：数据库访问
  - `UserMapper`: 用户相关数据库操作
  - `TimeSlotMapper`: 时间段相关数据库操作

- **工具类**：提供公共功能
  - `WechatUtils`: 微信相关工具方法
  - `XmlUtils`: XML解析工具方法

- **常量类**：集中管理常量值
  - `Constants`: 定义消息类型、事件类型等常量

## 系统功能

1. 接收微信用户消息和事件
2. 用户通过网页授权获取OpenID
3. 定时检查即将开始的任务
4. 在任务开始前15分钟发送提醒消息
5. 提醒消息包含任务标题、开始时间、结束时间和地点信息

## API文档

本项目使用Springdoc-OpenAPI自动生成API文档。启动应用后，可以通过以下URL访问API文档：

- Swagger UI界面: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

API文档中包含以下接口组：

1. **微信接口**
   - 验证微信服务器
   - 接收微信消息
   - 测试提醒消息

2. **微信网页授权**
   - 引导用户授权
   - 验证用户凭据
   - 处理授权回调

## 如何运行

1. 配置数据库
   ```sql
   CREATE DATABASE todoschedule;
   ```

2. 修改`application.properties`中的数据库配置

3. 运行应用
   ```bash
   ./mvnw spring-boot:run
   ```

4. 配置微信公众号
   - 在微信公众平台配置服务器URL为`http://您的域名/wechat`
   - 配置网页授权域名为您的域名

## 开发者

- 联系邮箱: contact@example.com
- 项目地址: https://github.com/example/todoschedule-server

## 系统概述

这是一个与微信公众号集成的任务提醒系统，主要功能包括：
1. 通过微信公众号发送任务开始前的提醒消息
2. 接收和响应用户通过公众号发送的消息

## 系统架构

系统主要由以下几个模块组成：

1. **微信消息处理** - 处理微信服务器的验证请求和消息交互
2. **任务提醒服务** - 定时检查即将开始的任务并发送提醒消息
3. **数据访问层** - 与数据库交互，获取用户和任务信息

## 数据模型

系统采用以下核心数据模型：

1. **时间槽 (time_slot)** 
   - 仅记录时间段信息，而非任务本身
   - `id`: 主键
   - `user_id`: 用户ID  
   - `start_time`: 开始时间（时间戳）
   - `end_time`: 结束时间（时间戳）
   - `schedule_type`: 日程类型（普通任务、课程等）
   - `schedule_id`: 关联到具体日程的ID
   - 通过`schedule_type`和`schedule_id`关联到实际任务

2. **普通日程 (ordinary_schedule)**
   - 存储普通任务的实际内容
   - `id`: 主键
   - `user_id`: 用户ID
   - `title`: 任务标题
   - `description`: 任务描述
   - `location`: 任务地点

3. **课程 (course)** 和 **课程节点 (course_node)**
   - 存储课程信息和具体的课程时间安排
   - 课程节点包含特定课程的上课时间、教室等信息

## 关键业务逻辑

### 时间段与任务的关系

1. **时间段(time_slot)不是任务**，而是时间信息的载体
2. 一个任务可以关联多个时间段（例如：重复任务、多时段课程）
3. 通过`schedule_type`字段区分时间段关联的是普通任务还是课程节点
4. 通过`schedule_id`字段关联到具体的普通任务或课程节点

### 任务提醒流程

1. 定时任务每分钟运行一次，检查即将开始的时间段
2. 查询在提醒窗口内（默认提前15分钟）开始且尚未通知的所有时间段
3. 根据时间段的类型和ID，查询关联的具体任务信息
4. 通过微信公众号向用户发送模板消息
5. 发送成功后将时间段的`is_notified`字段更新为`true`，避免重复发送

## 微信公众号配置

1. 配置微信公众号信息：
   - `wechat.mp.appId`: 公众号AppID
   - `wechat.mp.secret`: 公众号Secret
   - `wechat.mp.token`: 公众号Token
   - `wechat.mp.templates.reminder-template-id`: 提醒模板ID

2. 配置微信公众号服务器URL为：`http://your-domain.com/wechat`

## 系统配置与启动

1. 确保MySQL数据库已创建相应表结构
2. 在`application.properties`中配置数据库连接和微信信息
3. 启动应用：`mvn spring-boot:run`

## 测试功能

可通过访问以下端点测试提醒功能：
`/wechat/test-reminder?openid=YOUR_OPENID`

## 注意事项

1. 本系统默认数据库中已存在所需表结构
2. 提醒状态通过数据库中的`is_notified`字段管理，确保系统重启后也不会重复发送提醒
3. 生产环境中建议配置数据库备份

## 功能特点

1. **任务提醒**：在任务开始前自动发送提醒，包含任务标题、开始时间、结束时间和地点信息
2. **消息接收**：接收用户发送的文本消息并做出响应
3. **自动调度**：系统每分钟检查即将开始的时间段并发送相应任务提醒

## 配置步骤

### 1. 微信公众号配置

1. 登录微信公众平台测试号：https://mp.weixin.qq.com/debug/cgi-bin/sandbox?t=sandbox/login
2. 记录下AppID和AppSecret，填入`application.properties`文件中
3. 设置接口配置信息：
   - URL: `http://你的域名/wechat`（需要公网可访问）
   - Token: 与`application.properties`中的`wechat.mp.token`一致
4. 创建模板消息：
   - 标题：任务提醒
   - 内容模板：
   ```
   {{first.DATA}}
   任务名称：{{keyword1.DATA}}
   开始时间：{{keyword2.DATA}}
   结束时间：{{keyword3.DATA}}
   地点：{{keyword4.DATA}}
   {{remark.DATA}}
   ```
   - 记录下模板ID，填入`application.properties`中的`wechat.mp.templates.reminder-template-id`

### 2. 数据库配置

1. 确保MySQL数据库已启动并创建名为`todoschedule`的数据库
2. 更新`application.properties`中的数据库连接信息
3. 启动应用，系统会自动添加`is_notified`列到`time_slot`表

### 3. 运行应用

```bash
mvn spring-boot:run
```

### 4. 测试微信集成

1. 关注你的微信测试号
2. 向公众号发送消息，检查自动回复是否正常
3. 访问`http://localhost:8080/wechat/test-reminder?openid=用户的OPENID`测试任务提醒

## 注意事项

1. 请确保你的服务器能被微信服务器访问（通过公网IP或域名）
2. 在生产环境中，建议使用HTTPS
3. 当用户首次关注公众号时，会收到欢迎消息
4. 用户需要有正确的openid才能接收任务提醒，确保用户表中的openid字段已正确填写

## 示例代码

### 发送消息给用户
```java
wechatService.sendReminderMessage(
    user.getOpenid(), 
    "会议：项目进度讨论", 
    System.currentTimeMillis() + 15 * 60 * 1000, 
    System.currentTimeMillis() + 75 * 60 * 1000, 
    "会议室A"
);
```

### 接收用户消息
用户发送的消息会自动由系统处理，可以在`WechatServiceImpl`中的`processMessage`方法中定制消息处理逻辑。

## 项目结构

```
src/main/java/com/djj/todoscheduleserver/
├── config/                 # 配置类
│   ├── MyBatisConfig.java  # MyBatis配置
│   └── WechatMpConfig.java # 微信公众号配置
├── controller/
│   └── WechatController.java # 处理微信请求的控制器
├── mapper/                 # MyBatis映射接口
│   ├── TimeSlotMapper.java # 时间槽映射接口
│   └── UserMapper.java     # 用户映射接口
├── pojo/                   # 实体类
│   ├── TimeSlot.java       # 时间槽实体
│   ├── User.java           # 用户实体
│   └── wechat/             # 微信相关实体
│       ├── WxMpXmlMessage.java    # 微信XML消息
│       └── WxMpXmlOutMessage.java # 微信XML输出消息
├── service/                # 服务接口
│   ├── ReminderService.java  # 提醒服务接口
│   ├── WechatService.java    # 微信服务接口
│   └── impl/               # 服务实现
│       ├── ReminderServiceImpl.java # 提醒服务实现
│       └── WechatServiceImpl.java   # 微信服务实现
├── utils/                  # 工具类
│   ├── Constants.java      # 常量定义
│   ├── WechatUtils.java    # 微信工具类
│   └── XmlUtils.java       # XML处理工具类
└── TodoscheduleServerApplication.java # 应用入口
```

## 设计说明

### 核心组件

1. **WechatController**: 处理微信服务器验证和消息交互
2. **WechatService**: 处理消息处理和发送模板消息
3. **ReminderService**: 定时检查即将开始的任务并发送提醒
4. **MyBatis映射层**: 访问用户和任务数据

### 定时任务

系统使用Spring的`@Scheduled`注解实现定时任务，每分钟检查一次即将开始的时间段。默认提前15分钟发送提醒，可在配置文件中通过`