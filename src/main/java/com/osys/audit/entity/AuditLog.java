package com.osys.audit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志实体
 */
@Data
@TableName("audit_logs")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("msg_id")
    private String msgId;

    @TableField("msg_timestamp")
    private LocalDateTime msgTimestamp;

    @TableField("app_code")
    private String appCode;

    @TableField("service_name")
    private String serviceName;

    @TableField("module_name")
    private String moduleName;

    @TableField("event_type")
    private String eventType;

    @TableField("event_action")
    private String eventAction;

    @TableField("event_status")
    private Integer eventStatus;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("user_type")
    private String userType;

    @TableField("target_type")
    private String targetType;

    @TableField("target_id")
    private String targetId;

    @TableField("target_name")
    private String targetName;

    @TableField("operation_desc")
    private String operationDesc;

    @TableField("request_params")
    private String requestParams;

    @TableField("response_data")
    private String responseData;

    @TableField("old_values")
    private String oldValues;

    @TableField("new_values")
    private String newValues;

    @TableField("client_ip")
    private String clientIp;

    @TableField("user_agent")
    private String userAgent;

    @TableField("request_url")
    private String requestUrl;

    @TableField("request_method")
    private String requestMethod;

    @TableField("biz_no")
    private String bizNo;

    @TableField("trace_id")
    private String traceId;

    @TableField("extra_data")
    private String extraData;

    @TableField("risk_level")
    private Integer riskLevel;

    @TableField("risk_reason")
    private String riskReason;

    @TableField("partition_key")
    private Integer partitionKey;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 生成分区键 (YYYYMM)
     */
    public static Integer generatePartitionKey(LocalDateTime timestamp) {
        return timestamp.getYear() * 100 + timestamp.getMonthValue();
    }
}
