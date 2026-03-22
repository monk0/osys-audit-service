package com.osys.audit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计规则实体
 */
@Data
@TableName("audit_rules")
public class AuditRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("rule_code")
    private String ruleCode;

    @TableField("rule_name")
    private String ruleName;

    @TableField("rule_type")
    private String ruleType;

    @TableField("match_conditions")
    private String matchConditions;

    @TableField("action_type")
    private String actionType;

    @TableField("action_config")
    private String actionConfig;

    @TableField("risk_level")
    private Integer riskLevel;

    @TableField("status")
    private Integer status;

    @TableField("priority")
    private Integer priority;

    @TableField("hit_count")
    private Long hitCount;

    @TableField("last_hit_time")
    private LocalDateTime lastHitTime;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 增加命中次数
     */
    public void incrementHitCount() {
        if (this.hitCount == null) {
            this.hitCount = 0L;
        }
        this.hitCount++;
        this.lastHitTime = LocalDateTime.now();
    }

    /**
     * 检查规则是否启用
     */
    public boolean isEnabled() {
        return status != null && status == 1;
    }
}
