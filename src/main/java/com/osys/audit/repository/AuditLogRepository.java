package com.osys.audit.repository;

import com.osys.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 审计日志Repository
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Optional<AuditLog> findByMsgId(String msgId);

    List<AuditLog> findByAppCodeAndCreatedAtBetween(String appCode, LocalDateTime start, LocalDateTime end);

    List<AuditLog> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    List<AuditLog> findByTargetTypeAndTargetIdAndCreatedAtBetween(String targetType, String targetId, 
                                                                  LocalDateTime start, LocalDateTime end);

    List<AuditLog> findByEventTypeAndCreatedAtBetween(String eventType, LocalDateTime start, LocalDateTime end);

    List<AuditLog> findByRiskLevelGreaterThanAndCreatedAtBetween(Integer riskLevel, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.createdAt BETWEEN :start AND :end")
    Long countByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.riskLevel > 0 AND a.createdAt BETWEEN :start AND :end")
    Long countRiskByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT * FROM audit_logs WHERE partition_key = :partitionKey ORDER BY id DESC LIMIT :limit", nativeQuery = true)
    List<AuditLog> findRecentByPartition(@Param("partitionKey") Integer partitionKey, @Param("limit") Integer limit);
}
