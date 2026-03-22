package com.osys.audit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.osys.audit.entity.RiskEvent;
import com.osys.audit.mapper.RiskEventMapper;
import com.osys.audit.service.RiskEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 风险事件服务实现
 */
@Service
@RequiredArgsConstructor
public class RiskEventServiceImpl extends ServiceImpl<RiskEventMapper, RiskEvent> implements RiskEventService {

    private final RiskEventMapper riskEventMapper;

    @Override
    public List<RiskEvent> listByHandleStatus(Integer handleStatus, LocalDateTime start, LocalDateTime end) {
        return riskEventMapper.selectByHandleStatusAndTime(handleStatus, start, end);
    }

    @Override
    public List<RiskEvent> listUnhandledHighRisk(Integer riskLevel) {
        return riskEventMapper.selectByRiskLevelAndHandleStatus(riskLevel, 0);
    }

    @Override
    public Long countPending() {
        return riskEventMapper.countByHandleStatus(0);
    }

    @Override
    public void handleRisk(Long id, Long handlerId, String handlerName, String result) {
        RiskEvent riskEvent = getById(id);
        if (riskEvent != null) {
            riskEvent.handle(handlerId, handlerName, result);
            updateById(riskEvent);
        }
    }

    @Override
    public void ignoreRisk(Long id, Long handlerId, String handlerName, String reason) {
        RiskEvent riskEvent = getById(id);
        if (riskEvent != null) {
            riskEvent.ignore(handlerId, handlerName, reason);
            updateById(riskEvent);
        }
    }
}
