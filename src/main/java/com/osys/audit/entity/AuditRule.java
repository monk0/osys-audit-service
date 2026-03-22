package com.osys.audit.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 审计规则实体
 */
@Data
@Entity
@Table(name = "audit_rules", indexes = {
    @Index(name = "idx_rule_type", columnList = "ruleType"),
    @Index(name = "idx_status_priority", columnList = "status,priority")
})
public class AuditRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", unique = true, nullable = false, length = 50)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(name = "rule_type", nullable = false, length = 20)
    private String ruleType; // RISK/SENSITIVE/EXCEPTION

    @Column(name = "match_conditions", nullable = false, columnDefinition = "json")
    private String matchConditions; // JSON格式

    @Column(name = "action_type", nullable = false, length = 20)
    private String actionType; // ALERT/BLOCK/RECORD/NOTIFY

    @Column(name = "action_config", columnDefinition = "json")
    private String actionConfig;

    @Column(name = "risk_level")
    private Integer riskLevel = 1;

    @Column(name = "status")
    private Integer status = 1; // 0-禁用, 1-启用

    @Column(name = "priority")
    private Integer priority = 100;

    @Column(name = "hit_count")
    private Long hitCount = 0L;

    @Column(name = "last_hit_time")
    private LocalDateTime lastHitTime;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 增加命中次数
     */
    public void incrementHitCount() {
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
