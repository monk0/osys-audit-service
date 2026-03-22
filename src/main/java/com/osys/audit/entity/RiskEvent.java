package com.osys.audit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风险事件实体
 */
@Data
@TableName("risk_events")
public class RiskEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("audit_log_id")
    private Long auditLogId;

    @TableField("risk_level")
    private Integer riskLevel;

    @TableField("risk_type")
    private String riskType;

    @TableField("risk_desc")
    private String riskDesc;

    @TableField("hit_rules")
    private String hitRules;

    @TableField("handle_status")
    private Integer handleStatus;

    @TableField("handler_id")
    private Long handlerId;

    @TableField("handler_name")
    private String handlerName;

    @TableField("handle_result")
    private String handleResult;

    @TableField("handle_time")
    private LocalDateTime handleTime;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 处理风险事件
     */
    public void handle(Long handlerId, String handlerName, String result) {
        this.handleStatus = 2;
        this.handlerId = handlerId;
        this.handlerName = handlerName;
        this.handleResult = result;
        this.handleTime = LocalDateTime.now();
    }

    /**
     * 忽略风险事件
     */
    public void ignore(Long handlerId, String handlerName, String reason) {
        this.handleStatus = 3;
        this.handlerId = handlerId;
        this.handlerName = handlerName;
        this.handleResult = "忽略原因: " + reason;
        this.handleTime = LocalDateTime.now();
    }
}
