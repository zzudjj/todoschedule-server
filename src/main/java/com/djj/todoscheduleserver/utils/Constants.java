package com.djj.todoscheduleserver.utils;

/**
 * 常量类
 * 存储系统中使用的常量
 */
public class Constants {
    
    /**
     * 日程类型常量
     */
    public static class ScheduleType {
        public static final String ORDINARY = "ordinary";
        public static final String COURSE = "course";
        public static final String EXAM = "exam";
    }
    
    /**
     * 日程状态常量
     */
    public static class ScheduleStatus {
        public static final String TODO = "todo";
        public static final String DONE = "done";
        public static final String DOING = "doing";
        public static final String CANCELED = "canceled";
    }
    
    /**
     * 提醒类型常量
     */
    public static class ReminderType {
        public static final String NONE = "none";
        public static final String AT_TIME = "at_time";
        public static final String BEFORE = "before";
    }
    
    /**
     * 周类型常量
     */
    public static class WeekType {
        public static final int ALL = 0;  // A所有周
        public static final int ODD = 1;  // 单周
        public static final int EVEN = 2; // 双周
    }
    
    /**
     * HTTP响应状态码
     */
    public static class HttpStatus {
        public static final int OK = 200;
        public static final int BAD_REQUEST = 400;
        public static final int UNAUTHORIZED = 401;
        public static final int FORBIDDEN = 403;
        public static final int NOT_FOUND = 404;
        public static final int INTERNAL_SERVER_ERROR = 500;
    }
    
    /**
     * 同步类型常量
     */
    public static class SyncType {
        public static final String FULL = "full";
        public static final String DELTA = "delta";
    }
    
    /**
     * 微信消息类型常量
     */
    public static class WechatMsgType {
        public static final String TEXT = "text";
        public static final String IMAGE = "image";
        public static final String VOICE = "voice";
        public static final String VIDEO = "video";
        public static final String SHORT_VIDEO = "shortvideo";
        public static final String LOCATION = "location";
        public static final String LINK = "link";
        public static final String EVENT = "event";
    }
    
    /**
     * 微信事件类型常量
     */
    public static class WechatEventType {
        public static final String SUBSCRIBE = "subscribe";
        public static final String UNSUBSCRIBE = "unsubscribe";
        public static final String SCAN = "SCAN";
        public static final String LOCATION = "LOCATION";
        public static final String CLICK = "CLICK";
        public static final String VIEW = "VIEW";
    }
    
    /**
     * 消息类型常量
     * 使用简化命名方便引用
     */
    public static class MessageType {
        public static final String TEXT = "text";
        public static final String IMAGE = "image";
        public static final String VOICE = "voice";
        public static final String VIDEO = "video";
        public static final String SHORT_VIDEO = "shortvideo";
        public static final String LOCATION = "location";
        public static final String LINK = "link";
        public static final String EVENT = "event";
    }
    
    /**
     * 事件类型常量
     * 使用简化命名方便引用
     */
    public static class EventType {
        public static final String SUBSCRIBE = "subscribe";
        public static final String UNSUBSCRIBE = "unsubscribe";
        public static final String SCAN = "SCAN";
        public static final String LOCATION = "LOCATION";
        public static final String CLICK = "CLICK";
        public static final String VIEW = "VIEW";
    }
    
    /**
     * 时间常量（毫秒单位）
     */
    public static class TimeConstants {
        public static final long ONE_SECOND = 1000;
        public static final long ONE_MINUTE = 60 * ONE_SECOND;
        public static final long ONE_HOUR = 60 * ONE_MINUTE;
        public static final long ONE_DAY = 24 * ONE_HOUR;
        public static final long ONE_WEEK = 7 * ONE_DAY;
    }
} 