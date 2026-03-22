package com.osys.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.osys.audit.entity.AuditRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 审计规则Mapper
 */
@Mapper
public interface AuditRuleMapper extends BaseMapper<AuditRule> {

    /**
     * 查询启用的规则，按优先级排序
     */
    @Select("SELECT * FROM audit_rules WHERE status = #{status} ORDER BY priority ASC")
    List<AuditRule> selectByStatusOrderByPriority(Integer status);

    /**
     * 根据规则编码查询
     */
    @Select("SELECT * FROM audit_rules WHERE rule_code = #{ruleCode}")
    AuditRule selectByRuleCode(String ruleCode);
}
