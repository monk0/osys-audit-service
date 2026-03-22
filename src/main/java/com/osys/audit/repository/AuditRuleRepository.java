package com.osys.audit.repository;

import com.osys.audit.entity.AuditRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 审计规则Repository
 */
@Repository
public interface AuditRuleRepository extends JpaRepository<AuditRule, Long> {

    List<AuditRule> findByStatusOrderByPriorityAsc(Integer status);

    List<AuditRule> findByRuleTypeAndStatusOrderByPriorityAsc(String ruleType, Integer status);

    AuditRule findByRuleCode(String ruleCode);
}
