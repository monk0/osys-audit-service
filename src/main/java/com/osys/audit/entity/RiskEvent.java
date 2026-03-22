package com.osys.audit.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 风险事件实体
 */
@Data
@Entity
@Table(name = "risk_events", indexes = {
    @Index(name = "idx_risk_level", columnList = "riskLevel"),
    @Index(name = "idx_handle_status", columnList = "handleStatus"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
public class RiskEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_log_id", nullable = false)
    private Long auditLogId;

    @Column(name = "risk_level", nullable = false)
    private Integer riskLevel;

    @Column(name = "risk_type", nullable = false, length = 50)
    private String riskType;

    @Column(name = "risk_desc", length = 500)
    private String riskDesc;

    @Column(name = "hit_rules", columnDefinition = "json")
    private String hitRules;

    @Column(name = "handle_status")
    private Integer handleStatus = 0; // 0-未处理, 1-处理中, 2-已处理, 3-已忽略

    @Column(name = "handler_id")
    private Long handlerId;

    @Column(name = "handler_name", length = 64)
    private String handlerName;

    @Column(name = "handle_result", columnDefinition = "text")
    private String handleResult;

    @Column(name = "handle_time")
    private LocalDateTime handleTime;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
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
