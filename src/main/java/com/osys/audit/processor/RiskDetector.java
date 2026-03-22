package com.osys.audit.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.osys.audit.entity.AuditLog;
import com.osys.audit.entity.AuditRule;
import com.osys.audit.mq.AuditMessage;
import com.osys.audit.repository.AuditRuleRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * 风险检测器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskDetector {

    private final AuditRuleRepository auditRuleRepository;
    private final ObjectMapper objectMapper;
    
    private List<AuditRule> enabledRules = new ArrayList<>();

    @PostConstruct
    public void loadRules() {
        enabledRules = auditRuleRepository.findByStatusOrderByPriorityAsc(1);
        log.info("加载审计规则完成, 共{}条", enabledRules.size());
    }

    /**
     * 执行风险检测
     */
    public RiskResult detect(AuditLog auditLog, AuditMessage message) {
        RiskResult result = new RiskResult();
        
        for (AuditRule rule : enabledRules) {
            try {
                if (matchRule(rule, auditLog, message)) {
                    // 命中规则
                    result.addHitRule(rule);
                    
                    // 更新规则命中次数
                    rule.incrementHitCount();
                    auditRuleRepository.save(rule);
                    
                    log.debug("规则命中: ruleCode={}, auditLogId={}", rule.getRuleCode(), auditLog.getId());
                }
            } catch (Exception e) {
                log.error("规则匹配失败: ruleCode={}", rule.getRuleCode(), e);
            }
        }
        
        return result;
    }

    /**
     * 匹配规则
     */
    private boolean matchRule(AuditRule rule, AuditLog auditLog, AuditMessage message) {
        String conditions = rule.getMatchConditions();
        if (conditions == null || conditions.isEmpty()) {
            return false;
        }
        
        try {
            JsonNode conditionNode = objectMapper.readTree(conditions);
            
            // 匹配事件类型
            if (conditionNode.has("eventType")) {
                if (!conditionNode.get("eventType").asText().equals(auditLog.getEventType())) {
                    return false;
                }
            }
            
            // 匹配操作类型
            if (conditionNode.has("eventAction")) {
                if (!conditionNode.get("eventAction").asText().equals(auditLog.getEventAction())) {
                    return false;
                }
            }
            
            // 匹配应用编码
            if (conditionNode.has("appCode")) {
                if (!conditionNode.get("appCode").asText().equals(auditLog.getAppCode())) {
                    return false;
                }
            }
            
            // 匹配用户类型
            if (conditionNode.has("userType")) {
                if (!conditionNode.get("userType").asText().equals(auditLog.getUserType())) {
                    return false;
                }
            }
            
            // 匹配对象类型
            if (conditionNode.has("targetType")) {
                JsonNode targetTypes = conditionNode.get("targetType");
                if (targetTypes.isArray()) {
                    boolean match = false;
                    for (JsonNode type : targetTypes) {
                        if (type.asText().equals(auditLog.getTargetType())) {
                            match = true;
                            break;
                        }
                    }
                    if (!match) return false;
                } else {
                    if (!targetTypes.asText().equals(auditLog.getTargetType())) {
                        return false;
                    }
                }
            }
            
            // 匹配请求参数中的金额阈值
            if (conditionNode.has("requestParams.amount")) {
                String operator = conditionNode.get("requestParams.amount").asText();
                Double amount = extractAmount(auditLog.getRequestParams());
                if (amount == null || !compareAmount(amount, operator)) {
                    return false;
                }
            }
            
            return true;
            
        } catch (JsonProcessingException e) {
            log.error("解析规则条件失败: ruleCode={}", rule.getRuleCode(), e);
            return false;
        }
    }

    /**
     * 从JSON中提取金额
     */
    private Double extractAmount(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.has("amount")) {
                return node.get("amount").asDouble();
            }
        } catch (Exception e) {
            log.debug("提取金额失败: {}", json);
        }
        return null;
    }

    /**
     * 比较金额
     */
    private boolean compareAmount(double amount, String operator) {
        if (operator.startsWith(">=")) {
            return amount >= Double.parseDouble(operator.substring(2));
        } else if (operator.startsWith(">")) {
            return amount > Double.parseDouble(operator.substring(1));
        } else if (operator.startsWith("<=")) {
            return amount <= Double.parseDouble(operator.substring(2));
        } else if (operator.startsWith("<")) {
            return amount < Double.parseDouble(operator.substring(1));
        } else if (operator.startsWith("=")) {
            return amount == Double.parseDouble(operator.substring(1));
        }
        return false;
    }

    /**
     * 刷新规则
     */
    public void refreshRules() {
        enabledRules = auditRuleRepository.findByStatusOrderByPriorityAsc(1);
        log.info("刷新审计规则完成, 共{}条", enabledRules.size());
    }

    /**
     * 风险检测结果
     */
    @Data
    public static class RiskResult {
        private int riskLevel = 0;
        private String riskType;
        private String riskDesc;
        private List<String> hitRules = new ArrayList<>();

        public void addHitRule(AuditRule rule) {
            hitRules.add(rule.getRuleCode());
            if (rule.getRiskLevel() > riskLevel) {
                riskLevel = rule.getRiskLevel();
                riskType = rule.getRuleType();
                riskDesc = rule.getRuleName();
            }
        }

        public boolean hasRisk() {
            return riskLevel > 0;
        }
    }
}
