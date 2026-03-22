package com.osys.audit.mq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 审计消息DTO (MQ消息格式)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String msgId;
    private String msgVersion;
    private LocalDateTime timestamp;

    // 应用信息
    private AppInfo appInfo;

    // 事件信息
    private EventInfo event;

    // 操作人信息
    private OperatorInfo operator;

    // 操作对象
    private TargetInfo target;

    // 数据
    private DataInfo data;

    // 上下文
    private ContextInfo context;

    // 扩展数据
    private Map<String, Object> extra;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppInfo {
        private String appCode;
        private String serviceName;
        private String moduleName;
        private String instanceId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventInfo {
        private String type;
        private String action;
        private Integer status;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperatorInfo {
        private Long userId;
        private String userName;
        private String userType;
        private Long deptId;
        private String deptName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetInfo {
        private String type;
        private String id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataInfo {
        private Object requestParams;
        private Object oldValues;
        private Object newValues;
        private Object responseData;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContextInfo {
        private String clientIp;
        private String userAgent;
        private String requestUrl;
        private String requestMethod;
        private String traceId;
        private String bizNo;
        private String sessionId;
    }

    /**
     * 生成默认消息ID
     */
    public String generateMsgId() {
        return "audit-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 10000);
    }

    /**
     * 验证消息是否完整
     */
    public boolean isValid() {
        return appInfo != null && appInfo.getAppCode() != null
                && event != null && event.getType() != null
                && operator != null;
    }
}
