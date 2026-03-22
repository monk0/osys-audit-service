package com.osys.audit.repository;

import com.osys.audit.entity.RiskEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 风险事件Repository
 */
@Repository
public interface RiskEventRepository extends JpaRepository<RiskEvent, Long> {

    List<RiskEvent> findByHandleStatusAndCreatedAtBetween(Integer handleStatus, LocalDateTime start, LocalDateTime end);

    List<RiskEvent> findByRiskLevelGreaterThanEqualAndHandleStatusOrderByCreatedAtDesc(Integer riskLevel, Integer handleStatus);

    Long countByHandleStatus(Integer handleStatus);

    Long countByRiskLevelGreaterThanEqualAndCreatedAtBetween(Integer riskLevel, LocalDateTime start, LocalDateTime end);
}
