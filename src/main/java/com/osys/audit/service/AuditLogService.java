package com.osys.audit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.osys.audit.entity.AuditLog;
import com.osys.audit.mq.AuditMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审计日志服务接口
 */
public interface AuditLogService extends IService<AuditLog> {

    /**
     * 处理审计消息
     */
    void process(AuditMessage message);

    /**
     * 根据用户查询
     */
    List<AuditLog> queryByUser(Long userId, LocalDateTime start, LocalDateTime end);

    /**
     * 查询风险日志
     */
    List<AuditLog> queryRiskLogs(LocalDateTime start, LocalDateTime end);

    /**
     * 统计报表
     */
    Map<String, Object> statistics(LocalDateTime start, LocalDateTime end);
}
