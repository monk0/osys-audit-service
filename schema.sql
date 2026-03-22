# 审计服务数据库脚本

-- 创建数据库
CREATE DATABASE IF NOT EXISTS osys_audit 
    DEFAULT CHARACTER SET utf8mb4 
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE osys_audit;

-- ============================================
-- 1. 审计日志主表 (按月分区)
-- ============================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    msg_id              VARCHAR(64) NOT NULL COMMENT '消息唯一ID',
    msg_timestamp       DATETIME(3) NOT NULL COMMENT '消息产生时间',
    
    app_code            VARCHAR(50) NOT NULL COMMENT '应用编码',
    service_name        VARCHAR(100) NOT NULL COMMENT '服务名称',
    module_name         VARCHAR(100) COMMENT '模块名称',
    
    event_type          VARCHAR(50) NOT NULL COMMENT '事件类型',
    event_action        VARCHAR(50) NOT NULL COMMENT '操作: CREATE/UPDATE/DELETE/QUERY/LOGIN/LOGOUT',
    event_status        TINYINT DEFAULT 1 COMMENT '状态: 0-失败, 1-成功',
    
    user_id             BIGINT COMMENT '用户ID',
    user_name           VARCHAR(64) COMMENT '用户名',
    user_type           VARCHAR(20) COMMENT '用户类型: USER/ADMIN/SYSTEM',
    
    target_type         VARCHAR(50) COMMENT '对象类型',
    target_id           VARCHAR(64) COMMENT '对象ID',
    target_name         VARCHAR(200) COMMENT '对象名称',
    
    operation_desc      VARCHAR(500) COMMENT '操作描述',
    request_params      JSON COMMENT '请求参数',
    response_data       JSON COMMENT '响应数据',
    old_values          JSON COMMENT '变更前数据',
    new_values          JSON COMMENT '变更后数据',
    
    client_ip           VARCHAR(45) COMMENT '客户端IP',
    user_agent          VARCHAR(500) COMMENT 'UA',
    request_url         VARCHAR(500) COMMENT '请求URL',
    request_method      VARCHAR(10) COMMENT '请求方法',
    
    biz_no              VARCHAR(64) COMMENT '业务流水号',
    trace_id            VARCHAR(64) COMMENT '链路追踪ID',
    extra_data          JSON COMMENT '扩展数据',
    
    risk_level          TINYINT DEFAULT 0 COMMENT '风险等级: 0-正常, 1-低, 2-中, 3-高',
    risk_reason         VARCHAR(200) COMMENT '风险原因',
    
    partition_key       INT NOT NULL COMMENT '分区键: YYYYMM',
    created_at          DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    
    UNIQUE KEY uk_msg_id (msg_id),
    INDEX idx_app_code (app_code),
    INDEX idx_event_type (event_type),
    INDEX idx_user_id (user_id),
    INDEX idx_target (target_type, target_id),
    INDEX idx_biz_no (biz_no),
    INDEX idx_trace_id (trace_id),
    INDEX idx_created_at (created_at),
    INDEX idx_risk_level (risk_level),
    INDEX idx_partition_key (partition_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志主表';

-- 创建初始分区
ALTER TABLE audit_logs PARTITION BY RANGE (partition_key) (
    PARTITION p202403 VALUES LESS THAN (202404),
    PARTITION p202404 VALUES LESS THAN (202405),
    PARTITION p202405 VALUES LESS THAN (202406),
    PARTITION p_max VALUES LESS THAN MAXVALUE
);

-- ============================================
-- 2. 审计规则表
-- ============================================
CREATE TABLE IF NOT EXISTS audit_rules (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_code           VARCHAR(50) UNIQUE NOT NULL COMMENT '规则编码',
    rule_name           VARCHAR(100) NOT NULL COMMENT '规则名称',
    rule_type           VARCHAR(20) NOT NULL COMMENT '类型: RISK/SENSITIVE/EXCEPTION',
    match_conditions    JSON NOT NULL COMMENT '匹配条件',
    action_type         VARCHAR(20) NOT NULL COMMENT '动作: ALERT/BLOCK/RECORD/NOTIFY',
    action_config       JSON COMMENT '动作配置',
    risk_level          TINYINT DEFAULT 1 COMMENT '风险等级',
    status              TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    priority            INT DEFAULT 100 COMMENT '优先级',
    hit_count           BIGINT DEFAULT 0 COMMENT '命中次数',
    last_hit_time       DATETIME COMMENT '最后命中时间',
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_rule_type (rule_type),
    INDEX idx_status_priority (status, priority)
) COMMENT='审计规则表';

-- ============================================
-- 3. 风险事件表
-- ============================================
CREATE TABLE IF NOT EXISTS risk_events (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    audit_log_id        BIGINT NOT NULL COMMENT '审计日志ID',
    risk_level          TINYINT NOT NULL COMMENT '风险等级',
    risk_type           VARCHAR(50) NOT NULL COMMENT '风险类型',
    risk_desc           VARCHAR(500) COMMENT '风险描述',
    hit_rules           JSON COMMENT '命中的规则列表',
    handle_status       TINYINT DEFAULT 0 COMMENT '处理状态: 0-未处理, 1-处理中, 2-已处理, 3-已忽略',
    handler_id          BIGINT COMMENT '处理人ID',
    handler_name        VARCHAR(64) COMMENT '处理人',
    handle_result       TEXT COMMENT '处理结果',
    handle_time         DATETIME COMMENT '处理时间',
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_risk_level (risk_level),
    INDEX idx_handle_status (handle_status),
    INDEX idx_created_at (created_at)
) COMMENT='风险事件表';

-- ============================================
-- 4. 归档记录表
-- ============================================
CREATE TABLE IF NOT EXISTS archive_records (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    archive_date        DATE NOT NULL COMMENT '归档日期',
    partition_key       INT NOT NULL COMMENT '分区键',
    file_name           VARCHAR(200) NOT NULL COMMENT '归档文件名',
    file_path           VARCHAR(500) NOT NULL COMMENT '文件路径',
    record_count        BIGINT NOT NULL COMMENT '记录数',
    file_size           BIGINT COMMENT '文件大小(字节)',
    status              TINYINT DEFAULT 1 COMMENT '状态: 0-失败, 1-成功',
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_partition (partition_key),
    INDEX idx_archive_date (archive_date)
) COMMENT='归档记录表';

-- ============================================
-- 初始化审计规则
-- ============================================
INSERT INTO audit_rules (rule_code, rule_name, rule_type, match_conditions, action_type, risk_level, priority, status) VALUES
-- 大额支付预警
('LARGE_PAYMENT', '大额支付预警', 'RISK', '{"eventType": "PAYMENT", "requestParams.amount": "\u003e=100000"}', 'ALERT', 2, 10, 1),

-- 频繁登录检测
('FREQUENT_LOGIN', '频繁登录检测', 'RISK', '{"eventType": "LOGIN"}', 'ALERT', 1, 20, 1),

-- 敏感操作审计
('SENSITIVE_DELETE', '敏感数据删除', 'SENSITIVE', '{"eventAction": "DELETE", "targetType": ["USER", "ORDER", "PAYMENT"]}', 'ALERT', 2, 5, 1),

-- 异常时间操作
('OFFHOURS_OPERATION', '非工作时间操作', 'RISK', '{"hour": "\u003c6|\u003e22"}', 'RECORD', 1, 50, 1),

-- 管理员操作
('ADMIN_OPERATION', '管理员敏感操作', 'SENSITIVE', '{"userType": "ADMIN", "eventAction": ["DELETE", "UPDATE"]}', 'ALERT', 2, 15, 1)
ON DUPLICATE KEY UPDATE rule_name = VALUES(rule_name);
