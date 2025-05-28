package com.djj.todoscheduleserver.controller;

import com.djj.todoscheduleserver.common.Result;
import com.djj.todoscheduleserver.pojo.User;
import com.djj.todoscheduleserver.pojo.crdt.Device;
import com.djj.todoscheduleserver.pojo.crdt.SyncMessage;
import com.djj.todoscheduleserver.service.SyncService;
import com.djj.todoscheduleserver.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRDT数据同步控制器
 * 处理客户端与服务器之间的CRDT消息同步
 */
@Slf4j
@RestController
@RequestMapping("/sync")
@Tag(name = "CRDT同步接口", description = "处理基于CRDT的数据同步，服务器作为纯消息中继")
public class SyncController {

    @Autowired
    private SyncService syncService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 验证授权令牌的辅助方法
     * @param authorization 授权头部信息
     * @return 验证通过则返回User对象，否则返回null
     */
    private User validateToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("授权头部缺失或格式不正确");
            return null;
        }
        String token = authorization.substring(7); // 去掉"Bearer "前缀
        User user = userService.getUserByToken(token);
        if (user == null) {
            log.warn("根据token未找到用户: {}", token);
        }
        return user;
    }

    @PostMapping("/device/register")
    @Operation(summary = "注册设备", description = "客户端注册其设备ID和名称，服务器将返回设备信息，包括其上次同步的HLC时间戳。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "设备注册或更新成功",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = Device.class))),
            @ApiResponse(responseCode = "400", description = "请求体无效 (例如，缺少deviceId)",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "401", description = "未授权或令牌无效",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    })
    public ResponseEntity<?> registerDevice(
            @RequestHeader("Authorization") String authorization,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "设备注册请求体，包含deviceId和deviceName", required = true,
                            content = @Content(schema = @Schema(implementation = RegisterDeviceRequest.class)))
            @RequestBody RegisterDeviceRequest request) {
        
        User user = validateToken(authorization);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.unauthorized("无效的令牌或令牌已过期"));
        }

        if (request.getDeviceId() == null || request.getDeviceId().isEmpty()) {
            return ResponseEntity.badRequest().body(Result.badRequest("deviceId 不能为空"));
        }

        Device device = new Device();
        device.setId(request.getDeviceId());
        device.setName(request.getDeviceName());
        
        Device registeredDevice = syncService.registerDevice(device, authorization);
        log.info("用户 {} 的设备 {} 已注册/更新。", user.getId(), registeredDevice.getId());
        return ResponseEntity.ok(registeredDevice);
    }

    @PostMapping("/messages/{entityType}")
    @Operation(summary = "上传CRDT消息", description = "客户端上传特定实体类型的CRDT消息列表。")
    @Parameters({
            @Parameter(name = "Authorization", in = ParameterIn.HEADER, description = "认证令牌 (Bearer Token)", required = true, schema = @Schema(type = "string")),
            @Parameter(name = "X-Device-ID", in = ParameterIn.HEADER, description = "客户端设备ID", required = true, schema = @Schema(type = "string")),
            @Parameter(name = "entityType", in = ParameterIn.PATH, description = "实体类型 (例如: OrdinarySchedule, Course, TimeSlot等)", required = true, schema = @Schema(type = "string"))
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "消息接收并开始处理",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "400", description = "请求体无效或缺少头部信息",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "401", description = "未授权或令牌无效",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    })
    public ResponseEntity<Result<Void>> uploadMessages(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("X-Device-ID") String deviceId,
            @PathVariable String entityType,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "JSON格式的CRDT消息字符串列表", required = true,
                            content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = String.class)))
            @RequestBody List<String> messages) {
        log.info("上传消息");
        User user = validateToken(authorization);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.unauthorized("无效的令牌或令牌已过期"));
        }
        if (deviceId == null || deviceId.isEmpty()) {
            log.info(deviceId);
            log.info("X-Device-ID 头部信息不能为空");
            return ResponseEntity.badRequest().body(Result.badRequest("X-Device-ID 头部信息不能为空"));
        }
        if (messages == null || messages.isEmpty()) {
            for (String message : messages) {
                log.info(message);
            }
            log.info("消息列表为空");
            return ResponseEntity.badRequest().body(Result.badRequest("消息列表不能为空"));
        }

        syncService.storeClientMessages(user, deviceId, entityType, messages);
        log.info("用户 {} 从设备 {} 上传了 {} 条 {} 类型的消息。", user.getId(), deviceId, messages.size(), entityType);
        return ResponseEntity.ok(Result.success("消息已接收"));
    }

    @GetMapping("/messages/all")
    @Operation(summary = "下载所有类型的CRDT消息", description = "客户端下载自上次同步以来服务器上所有实体类型的CRDT消息。")
    @Parameters({
            @Parameter(name = "Authorization", in = ParameterIn.HEADER, description = "认证令牌 (Bearer Token)", required = true, schema = @Schema(type = "string")),
            @Parameter(name = "X-Device-ID", in = ParameterIn.HEADER, description = "客户端设备ID", required = true, schema = @Schema(type = "string")),
            @Parameter(name = "since", in = ParameterIn.QUERY, description = "HLC时间戳，表示从哪个时间点之后开始获取消息。如果未提供，则从设备上次记录的HLC或0开始。", required = false, schema = @Schema(type = "integer", format = "int64"))
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "消息下载成功",
                         content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = SyncMessage.class))),
            @ApiResponse(responseCode = "401", description = "未授权或令牌无效",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    })
    public ResponseEntity<?> downloadAllMessages(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("X-Device-ID") String deviceId,
            @RequestParam(required = false) Long since) {
        
        User user = validateToken(authorization);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.unauthorized("无效的令牌或令牌已过期"));
        }
        if (deviceId == null || deviceId.isEmpty()) {
            return ResponseEntity.badRequest().body(Result.badRequest("X-Device-ID 头部信息不能为空"));
        }

        List<SyncMessage> messages = syncService.getMessagesForDevice(user, deviceId, since);
        log.info("用户 {} 的设备 {} 请求下载所有类型消息，自HLC: {}。返回 {} 条消息。", user.getId(), deviceId, since, messages.size());
        return ResponseEntity.ok(messages);
    }
    
    @GetMapping("/messages/all/exclude-origin")
    @Operation(summary = "下载所有类型的CRDT消息（排除本设备发出的消息）", description = "客户端下载自上次同步以来服务器上所有实体类型的CRDT消息，排除来自当前设备的消息。")
    @Parameters({
            @Parameter(name = "Authorization", in = ParameterIn.HEADER, description = "认证令牌 (Bearer Token)", required = true, schema = @Schema(type = "string")),
            @Parameter(name = "X-Device-ID", in = ParameterIn.HEADER, description = "客户端设备ID", required = true, schema = @Schema(type = "string")),
            @Parameter(name = "since", in = ParameterIn.QUERY, description = "HLC时间戳，表示从哪个时间点之后开始获取消息。如果未提供，则从设备上次记录的HLC或0开始。", required = false, schema = @Schema(type = "integer", format = "int64"))
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "消息下载成功",
                         content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = SyncMessage.class))),
            @ApiResponse(responseCode = "401", description = "未授权或令牌无效",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    })
    public ResponseEntity<?> downloadAllMessagesExcludingOrigin(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("X-Device-ID") String deviceId,
            @RequestParam(required = false) Long since) {
        
        User user = validateToken(authorization);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.unauthorized("无效的令牌或令牌已过期"));
        }
        if (deviceId == null || deviceId.isEmpty()) {
            return ResponseEntity.badRequest().body(Result.badRequest("X-Device-ID 头部信息不能为空"));
        }

        List<SyncMessage> messages = syncService.getMessagesAfterTimestampExcludingOriginDevice(user, deviceId, since);
        log.info("用户 {} 的设备 {} 请求下载所有类型消息（排除自己发出的消息），自HLC: {}。返回 {} 条消息。", user.getId(), deviceId, since, messages.size());
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/messages/{entityType}")
    @Operation(summary = "下载特定实体类型的CRDT消息", description = "客户端下载自上次同步以来服务器上特定实体类型的CRDT消息。")
    @Parameters({
            @Parameter(name = "Authorization", in = ParameterIn.HEADER, description = "认证令牌 (Bearer Token)", required = true, schema = @Schema(type = "string")),
            @Parameter(name = "X-Device-ID", in = ParameterIn.HEADER, description = "客户端设备ID", required = true, schema = @Schema(type = "string")),
            @Parameter(name = "entityType", in = ParameterIn.PATH, description = "实体类型 (例如: OrdinarySchedule, Course, TimeSlot等)", required = true, schema = @Schema(type = "string")),
            @Parameter(name = "since", in = ParameterIn.QUERY, description = "HLC时间戳，表示从哪个时间点之后开始获取消息。如果未提供，则从设备上次记录的HLC或0开始。", required = false, schema = @Schema(type = "integer", format = "int64"))
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "消息下载成功",
                         content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = SyncMessage.class))),
            @ApiResponse(responseCode = "401", description = "未授权或令牌无效",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    })
    public ResponseEntity<?> downloadMessagesByType(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("X-Device-ID") String deviceId,
            @PathVariable String entityType,
            @RequestParam(required = false) Long since) {
        
        User user = validateToken(authorization);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.unauthorized("无效的令牌或令牌已过期"));
        }
         if (deviceId == null || deviceId.isEmpty()) {
            return ResponseEntity.badRequest().body(Result.badRequest("X-Device-ID 头部信息不能为空"));
        }

        List<SyncMessage> messages = syncService.getMessagesByEntityTypeForDevice(user, deviceId, entityType, since);
        log.info("用户 {} 的设备 {} 请求下载 {} 类型消息，自HLC: {}。返回 {} 条消息。", user.getId(), deviceId, entityType, since, messages.size());
        return ResponseEntity.ok(messages);
    }
    
    @GetMapping("/messages/{entityType}/exclude-origin")
    @Operation(summary = "下载特定实体类型的CRDT消息（排除本设备发出的消息）", description = "客户端下载自上次同步以来服务器上特定实体类型的CRDT消息，排除来自当前设备的消息。")
    @Parameters({
            @Parameter(name = "Authorization", in = ParameterIn.HEADER, description = "认证令牌 (Bearer Token)", required = true, schema = @Schema(type = "string")),
            @Parameter(name = "X-Device-ID", in = ParameterIn.HEADER, description = "客户端设备ID", required = true, schema = @Schema(type = "string")),
            @Parameter(name = "entityType", in = ParameterIn.PATH, description = "实体类型 (例如: OrdinarySchedule, Course, TimeSlot等)", required = true, schema = @Schema(type = "string")),
            @Parameter(name = "since", in = ParameterIn.QUERY, description = "HLC时间戳，表示从哪个时间点之后开始获取消息。如果未提供，则从设备上次记录的HLC或0开始。", required = false, schema = @Schema(type = "integer", format = "int64"))
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "消息下载成功",
                         content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = SyncMessage.class))),
            @ApiResponse(responseCode = "401", description = "未授权或令牌无效",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    })
    public ResponseEntity<?> downloadMessagesByTypeExcludingOrigin(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("X-Device-ID") String deviceId,
            @PathVariable String entityType,
            @RequestParam(required = false) Long since) {
        
        User user = validateToken(authorization);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.unauthorized("无效的令牌或令牌已过期"));
        }
         if (deviceId == null || deviceId.isEmpty()) {
            return ResponseEntity.badRequest().body(Result.badRequest("X-Device-ID 头部信息不能为空"));
        }

        List<SyncMessage> messages = syncService.getMessagesByEntityTypeAfterTimestampExcludingOriginDevice(user, deviceId, entityType, since);
        log.info("用户 {} 的设备 {} 请求下载 {} 类型消息（排除自己发出的消息），自HLC: {}。返回 {} 条消息。", user.getId(), deviceId, entityType, since, messages.size());
        return ResponseEntity.ok(messages);
    }

    // 辅助内部类，用于清晰地定义/device/register的请求体
    @Schema(description = "设备注册请求体")
    static class RegisterDeviceRequest {
        @Schema(description = "客户端生成的唯一设备ID", required = true)
        private String deviceId;
        @Schema(description = "设备的用户友好名称（可选）")
        private String deviceName;

        public String getDeviceId() {
            return deviceId;
        }
        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }
        public String getDeviceName() {
            return deviceName;
        }
        public void setDeviceName(String deviceName) {
            this.deviceName = deviceName;
        }
    }

    /**
     * 获取设备列表
     */
    @GetMapping("/devices")
    @Operation(summary = "获取设备列表", description = "获取当前用户的所有已注册设备")
    public Result<List<Device>> getDevices(@RequestHeader("Authorization") String token) {
        return Result.success(syncService.getDevicesByToken(token));
    }

    /**
     * 提交同步消息
     */
    @PostMapping("/messages")
    @Operation(summary = "批量提交CRDT消息", description = "批量提交各种类型的CRDT消息")
    public Result<Void> submitMessages(@RequestBody List<SyncMessage> messages, @RequestHeader("Authorization") String token) {
        syncService.processMessages(messages, token);
        return Result.success("");
    }
} 