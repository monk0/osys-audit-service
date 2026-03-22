package com.osys.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osys.audit.entity.AuditLog;
import com.osys.audit.entity.AuditRule;
import com.osys.audit.entity.RiskEvent;
import com.osys.audit.mq.AuditMessage;
import com.osys.audit.processor.RiskDetector;
import com.osys.audit.repository.AuditLogRepository;
import com.osys.audit.repository.RiskEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审计日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final RiskEventRepository riskEventRepository;
    private final RiskDetector riskDetector;
    private final ObjectMapper objectMapper;

    /**
     * 处理审计消息
     */
    @Transactional
    public void process(AuditMessage message) {
        // 1. 转换为实体
        AuditLog auditLog = convertToEntity(message);
        
        // 2. 生成分区键
        auditLog.setPartitionKey(AuditLog.generatePartitionKey(auditLog.getMsgTimestamp()));
        
        // 3. 保存审计日志
        auditLogRepository.save(auditLog);
        
        // 4. 风险检测
        RiskDetector.RiskResult riskResult = riskDetector.detect(auditLog, message);
        
        // 5. 如有风险,创建风险事件
        if (riskResult.hasRisk()) {
            createRiskEvent(auditLog, riskResult);
            
            // 更新审计日志风险等级
            auditLog.setRiskLevel(riskResult.getRiskLevel());
            auditLog.setRiskReason(riskResult.getRiskDesc());
            auditLogRepository.save(auditLog);
        }
        
        log.debug("审计消息处理完成: {}, 风险等级: {}", message.getMsgId(), auditLog.getRiskLevel());
    }

    /**
     * 转换为实体
     */
    private AuditLog convertToEntity(AuditMessage message) {
        AuditLog entity = new AuditLog();
        
        entity.setMsgId(message.getMsgId());
        entity.setMsgTimestamp(message.getTimestamp() != null ? message.getTimestamp() : LocalDateTime.now());
        
        // 应用信息
        if (message.getAppInfo() != null) {
            entity.setAppCode(message.getAppInfo().getAppCode());
            entity.setServiceName(message.getAppInfo().getServiceName());
            entity.setModuleName(message.getAppInfo().getModuleName());
        }
        
        // 事件信息
        if (message.getEvent() != null) {
            entity.setEventType(message.getEvent().getType());
            entity.setEventAction(message.getEvent().getAction());
            entity.setEventStatus(message.getEvent().getStatus());
            entity.setOperationDesc(message.getEvent().getDescription());
        }
        
        // 操作人信息
        if (message.getOperator() != null) {
            entity.setUserId(message.getOperator().getUserId());
            entity.setUserName(message.getOperator().getUserName());
            entity.setUserType(message.getOperator().getUserType());
        }
        
        // 操作对象
        if (message.getTarget() != null) {
            entity.setTargetType(message.getTarget().getType());
            entity.setTargetId(message.getTarget().getId());
            entity.setTargetName(message.getTarget().getName());
        }
        
        // 数据
        if (message.getData() != null) {
            entity.setRequestParams(toJson(message.getData().getRequestParams()));
            entity.setResponseData(toJson(message.getData().getResponseData()));
            entity.setOldValues(toJson(message.getData().getOldValues()));
            entity.setNewValues(toJson(message.getData().getNewValues()));
        }
        
        // 上下文
        if (message.getContext() != null) {
            entity.setClientIp(message.getContext().getClientIp());
            entity.setUserAgent(message.getContext().getUserAgent());
            entity.setRequestUrl(message.getContext().getRequestUrl());
            entity.setRequestMethod(message.getContext().getRequestMethod());
            entity.setTraceId(message.getContext().getTraceId());
            entity.setBizNo(message.getContext().getBizNo());
        }
        
        // 扩展数据
        entity.setExtraData(toJson(message.getExtra()));
        
        return entity;
    }

    /**
     * 创建风险事件
     */
    private void createRiskEvent(AuditLog auditLog, RiskDetector.RiskResult riskResult) {
        RiskEvent riskEvent = new RiskEvent();
        riskEvent.setAuditLogId(auditLog.getId());
        riskEvent.setRiskLevel(riskResult.getRiskLevel());
        riskEvent.setRiskType(riskResult.getRiskType());
        riskEvent.setRiskDesc(riskResult.getRiskDesc());
        riskEvent.setHitRules(toJson(riskResult.getHitRules()));
        
        riskEventRepository.save(riskEvent);
        
        log.warn("风险事件创建: auditLogId={}, riskLevel={}, desc={}", 
                auditLog.getId(), riskResult.getRiskLevel(), riskResult.getRiskDesc());
    }

    /**
     * 对象转JSON
     */
    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("JSON序列化失败", e);
            return obj.toString();
        }
    }

    /**
     * 查询审计日志
     */
    public List<AuditLog> queryByUser(Long userId, LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByUserIdAndCreatedAtBetween(userId, start, end);
    }

    /**
     * 查询风险审计日志
     */
    public List<AuditLog> queryRiskLogs(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByRiskLevelGreaterThanAndCreatedAtBetween(0, start, end);
    }

    /**
     * 统计报表
     */
    public Map<String, Object> statistics(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalCount", auditLogRepository.countByCreatedAtBetween(start, end));
        stats.put("riskCount", auditLogRepository.countRiskByCreatedAtBetween(start, end));
        stats.put("pendingRiskCount", riskEventRepository.countByHandleStatus(0));
        return stats;
    }
}
