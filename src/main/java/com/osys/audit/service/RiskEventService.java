package com.osys.audit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.osys.audit.entity.RiskEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 风险事件服务接口
 */
public interface RiskEventService extends IService<RiskEvent> {

    /**
     * 根据处理状态查询
     */
    List<RiskEvent> listByHandleStatus(Integer handleStatus, LocalDateTime start, LocalDateTime end);

    /**
     * 查询高等级未处理风险
     */
    List<RiskEvent> listUnhandledHighRisk(Integer riskLevel);

    /**
     * 统计待处理数量
     */
    Long countPending();

    /**
     * 处理风险事件
     */
    void handleRisk(Long id, Long handlerId, String handlerName, String result);

    /**
     * 忽略风险事件
     */
    void ignoreRisk(Long id, Long handlerId, String handlerName, String reason);
}
