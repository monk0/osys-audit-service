package com.osys.audit.controller;

import com.osys.audit.entity.AuditLog;
import com.osys.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审计日志查询接口
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * 根据用户查询
     */
    @GetMapping("/user/{userId}")
    public List<AuditLog> queryByUser(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        return auditLogService.queryByUser(userId, start, end);
    }

    /**
     * 查询风险日志
     */
    @GetMapping("/risk")
    public List<AuditLog> queryRiskLogs(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        return auditLogService.queryRiskLogs(start, end);
    }

    /**
     * 统计报表
     */
    @GetMapping("/statistics")
    public Map<String, Object> statistics(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        return auditLogService.statistics(start, end);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "audit-service");
        return result;
    }
}
