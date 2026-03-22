package com.osys.audit.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 审计日志实体
 */
@Data
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_app_code", columnList = "appCode"),
    @Index(name = "idx_event_type", columnList = "eventType"),
    @Index(name = "idx_user_id", columnList = "userId"),
    @Index(name = "idx_target", columnList = "targetType,targetId"),
    @Index(name = "idx_biz_no", columnList = "bizNo"),
    @Index(name = "idx_trace_id", columnList = "traceId"),
    @Index(name = "idx_created_at", columnList = "createdAt"),
    @Index(name = "idx_risk_level", columnList = "riskLevel"),
    @Index(name = "idx_partition_key", columnList = "partitionKey")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "msg_id", unique = true, nullable = false, length = 64)
    private String msgId;

    @Column(name = "msg_timestamp", nullable = false)
    private LocalDateTime msgTimestamp;

    @Column(name = "app_code", nullable = false, length = 50)
    private String appCode;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(name = "module_name", length = 100)
    private String moduleName;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_action", nullable = false, length = 50)
    private String eventAction;

    @Column(name = "event_status")
    private Integer eventStatus = 1;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name", length = 64)
    private String userName;

    @Column(name = "user_type", length = 20)
    private String userType;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id", length = 64)
    private String targetId;

    @Column(name = "target_name", length = 200)
    private String targetName;

    @Column(name = "operation_desc", length = 500)
    private String operationDesc;

    @Column(name = "request_params", columnDefinition = "json")
    private String requestParams;

    @Column(name = "response_data", columnDefinition = "json")
    private String responseData;

    @Column(name = "old_values", columnDefinition = "json")
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "json")
    private String newValues;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "request_url", length = 500)
    private String requestUrl;

    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(name = "biz_no", length = 64)
    private String bizNo;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "extra_data", columnDefinition = "json")
    private String extraData;

    @Column(name = "risk_level")
    private Integer riskLevel = 0;

    @Column(name = "risk_reason", length = 200)
    private String riskReason;

    @Column(name = "partition_key", nullable = false)
    private Integer partitionKey;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 生成分区键 (YYYYMM)
     */
    public static Integer generatePartitionKey(LocalDateTime timestamp) {
        return timestamp.getYear() * 100 + timestamp.getMonthValue();
    }

    /**
     * 获取脱敏后的请求参数
     */
    public String getMaskedRequestParams() {
        // 实际实现中调用脱敏工具
        return requestParams;
    }
}
