package com.osys.audit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.osys.audit.entity.AuditRule;

import java.util.List;

/**
 * 审计规则服务接口
 */
public interface AuditRuleService extends IService<AuditRule> {

    /**
     * 查询启用的规则
     */
    List<AuditRule> listEnabledRules();

    /**
     * 根据编码查询
     */
    AuditRule getByCode(String ruleCode);

    /**
     * 刷新规则缓存
     */
    void refreshRules();
}
