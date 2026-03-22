package com.osys.audit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.osys.audit.entity.AuditRule;
import com.osys.audit.mapper.AuditRuleMapper;
import com.osys.audit.service.AuditRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 审计规则服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditRuleServiceImpl extends ServiceImpl<AuditRuleMapper, AuditRule> implements AuditRuleService {

    private final AuditRuleMapper auditRuleMapper;

    // 本地缓存
    private List<AuditRule> enabledRulesCache = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        refreshRules();
    }

    @Override
    public List<AuditRule> listEnabledRules() {
        return enabledRulesCache;
    }

    @Override
    public AuditRule getByCode(String ruleCode) {
        return auditRuleMapper.selectByRuleCode(ruleCode);
    }

    @Override
    public void refreshRules() {
        enabledRulesCache = auditRuleMapper.selectByStatusOrderByPriority(1);
        log.info("刷新审计规则缓存完成, 共{}条", enabledRulesCache.size());
    }
}
