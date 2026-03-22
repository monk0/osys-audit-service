# 独立审计服务设计方案

## 架构概述

```
┌─────────────────────────────────────────────────────────────────┐
│                        业务系统集群                                │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │ 订单服务  │  │ 用户服务  │  │ 支付服务  │  │ 库存服务  │        │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘        │
└───────┼─────────────┼─────────────┼─────────────┼────────────────┘
        │             │             │             │
        └─────────────┴─────────────┴─────────────┘
                          │ 发送审计消息
                          ▼
        ┌───────────────────────────────────────┐
        │         消息队列 (RabbitMQ/Kafka)       │
        │  Topic: audit-log                      │
        └───────────────────┬───────────────────┘
                            │ 消费审计消息
                            ▼
        ┌───────────────────────────────────────┐
        │         审计服务 (Audit Service)       │
        │  - 消息消费                            │
        │  - 数据清洗/格式化                      │
        │  - 规则引擎（风险检测）                  │
        │  - 数据存储                            │
        └───────────────────┬───────────────────┘
                            │ 写入
              ┌─────────────┼─────────────┐
              ▼             ▼             ▼
        ┌──────────┐  ┌──────────┐  ┌──────────┐
        │MySQL     │  │ES        │  │OSS/S3    │
        │(结构化)   │  │(全文检索) │  │(归档)     │
        └──────────┘  └──────────┘  └──────────┘
```

## 核心设计原则

### 1. 完全解耦
- 业务系统只负责**发送消息**，不关心审计服务状态
- 审计服务独立部署、独立扩展
- 审计服务故障不影响业务系统

### 2. 异步处理
- 近实时处理（秒级延迟）
- 批量消费提升性能
- 支持削峰填谷

### 3. 可靠性
- 消息持久化保证不丢失
- 消费失败可重试
- 死信队列处理异常消息

### 4. 扩展性
- 支持水平扩展
- 支持按业务分流
- 支持自定义审计规则

---

## 数据库设计

### 审计日志主表 (audit_logs)

```sql
CREATE TABLE audit_logs (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    
    -- 消息元数据
    msg_id              VARCHAR(64) NOT NULL COMMENT '消息唯一ID',
    msg_timestamp       DATETIME NOT NULL COMMENT '消息产生时间',
    
    -- 业务信息
    app_code            VARCHAR(50) NOT NULL COMMENT '应用编码',
    service_name        VARCHAR(100) NOT NULL COMMENT '服务名称',
    module_name         VARCHAR(100) COMMENT '模块名称',
    
    -- 审计事件
    event_type          VARCHAR(50) NOT NULL COMMENT '事件类型: LOGIN/ORDER_CREATE/PAYMENT/...',
    event_action        VARCHAR(50) NOT NULL COMMENT '操作: CREATE/UPDATE/DELETE/QUERY/LOGIN/LOGOUT',
    event_status        TINYINT DEFAULT 1 COMMENT '状态: 0-失败, 1-成功',
    
    -- 操作人信息
    user_id             BIGINT COMMENT '用户ID',
    user_name           VARCHAR(64) COMMENT '用户名',
    user_type           VARCHAR(20) COMMENT '用户类型: USER/ADMIN/SYSTEM',
    
    -- 操作对象
    target_type         VARCHAR(50) COMMENT '对象类型: USER/ORDER/PRODUCT/...',
    target_id           VARCHAR(64) COMMENT '对象ID',
    target_name         VARCHAR(200) COMMENT '对象名称',
    
    -- 操作详情
    operation_desc      VARCHAR(500) COMMENT '操作描述',
    request_params      JSON COMMENT '请求参数',
    response_data       JSON COMMENT '响应数据(脱敏后)',
    old_values          JSON COMMENT '变更前数据',
    new_values          JSON COMMENT '变更后数据',
    
    -- 环境信息
    client_ip           VARCHAR(45) COMMENT '客户端IP',
    user_agent          VARCHAR(500) COMMENT 'UA',
    request_url         VARCHAR(500) COMMENT '请求URL',
    request_method      VARCHAR(10) COMMENT '请求方法',
    
    -- 扩展字段
    biz_no              VARCHAR(64) COMMENT '业务流水号',
    trace_id            VARCHAR(64) COMMENT '链路追踪ID',
    extra_data          JSON COMMENT '扩展数据',
    
    -- 风险标记
    risk_level          TINYINT DEFAULT 0 COMMENT '风险等级: 0-正常, 1-低, 2-中, 3-高',
    risk_reason         VARCHAR(200) COMMENT '风险原因',
    
    -- 存储分区字段(按月分区)
    partition_key       INT NOT NULL COMMENT '分区键: YYYYMM',
    
    -- 时间戳
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    -- 索引
    INDEX idx_app_code (app_code),
    INDEX idx_event_type (event_type),
    INDEX idx_user_id (user_id),
    INDEX idx_target (target_type, target_id),
    INDEX idx_biz_no (biz_no),
    INDEX idx_trace_id (trace_id),
    INDEX idx_created_at (created_at),
    INDEX idx_risk_level (risk_level),
    INDEX idx_partition_key (partition_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志主表'
PARTITION BY RANGE (partition_key) (
    PARTITION p202401 VALUES LESS THAN (202402),
    PARTITION p202402 VALUES LESS THAN (202403),
    -- 每月自动创建分区
);
```

### 审计规则表 (audit_rules)

```sql
CREATE TABLE audit_rules (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_code           VARCHAR(50) UNIQUE NOT NULL COMMENT '规则编码',
    rule_name           VARCHAR(100) NOT NULL COMMENT '规则名称',
    rule_type           VARCHAR(20) NOT NULL COMMENT '类型: RISK/SENSITIVE/EXCEPTION',
    
    -- 匹配条件(JSON)
    match_conditions    JSON NOT NULL COMMENT '匹配条件',
    -- 示例: {"appCode": "order-service", "eventType": "PAYMENT", "amount": ">10000"}
    
    -- 触发动作
    action_type         VARCHAR(20) NOT NULL COMMENT '动作: ALERT/BLOCK/RECORD/NOTIFY',
    action_config       JSON COMMENT '动作配置',
    
    -- 风险等级
    risk_level          TINYINT DEFAULT 1 COMMENT '风险等级: 1-低, 2-中, 3-高',
    
    -- 状态
    status              TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    priority            INT DEFAULT 100 COMMENT '优先级(越小越优先)',
    
    -- 统计
    hit_count           BIGINT DEFAULT 0 COMMENT '命中次数',
    last_hit_time       DATETIME COMMENT '最后命中时间',
    
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_rule_type (rule_type),
    INDEX idx_status_priority (status, priority)
) COMMENT='审计规则表';
```

### 风险事件表 (risk_events)

```sql
CREATE TABLE risk_events (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- 关联审计日志
    audit_log_id        BIGINT NOT NULL COMMENT '审计日志ID',
    
    -- 风险信息
    risk_level          TINYINT NOT NULL COMMENT '风险等级',
    risk_type           VARCHAR(50) NOT NULL COMMENT '风险类型',
    risk_desc           VARCHAR(500) COMMENT '风险描述',
    hit_rules           JSON COMMENT '命中的规则列表',
    
    -- 处理状态
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
```

---

## MQ 消息格式

### 审计消息标准格式

```json
{
  "msgId": "audit-202403220001-abc123",
  "msgVersion": "1.0",
  "timestamp": "2024-03-22T11:22:33.456+08:00",
  
  "appInfo": {
    "appCode": "order-service",
    "serviceName": "OrderService",
    "moduleName": "order-create",
    "instanceId": "order-service-01"
  },
  
  "event": {
    "type": "ORDER_CREATE",
    "action": "CREATE",
    "status": 1,
    "description": "创建订单"
  },
  
  "operator": {
    "userId": 10001,
    "userName": "张三",
    "userType": "USER",
    "deptId": 101,
    "deptName": "销售部"
  },
  
  "target": {
    "type": "ORDER",
    "id": "ORD202403220001",
    "name": "订单-笔记本电脑"
  },
  
  "data": {
    "requestParams": {
      "productId": 1001,
      "quantity": 2,
      "amount": 19998.00
    },
    "oldValues": null,
    "newValues": {
      "orderId": "ORD202403220001",
      "status": "PENDING_PAY",
      "amount": 19998.00
    },
    "responseData": {
      "success": true,
      "orderId": "ORD202403220001"
    }
  },
  
  "context": {
    "clientIp": "192.168.1.100",
    "userAgent": "Mozilla/5.0...",
    "requestUrl": "/api/v1/orders",
    "requestMethod": "POST",
    "traceId": "trace-abc123",
    "bizNo": "BIZ202403220001",
    "sessionId": "sess-xyz789"
  },
  
  "extra": {
    "source": "APP",
    "deviceId": "device-001",
    "geoLocation": "北京市朝阳区"
  }
}
```

---

## 技术架构

### 1. 消息队列选型

| 特性 | RabbitMQ | Kafka | RocketMQ |
|------|----------|-------|----------|
| 吞吐量 | 万级 | 十万级 | 十万级 |
| 延迟 | 微秒级 | 毫秒级 | 毫秒级 |
| 可靠性 | 高 | 极高 | 极高 |
| 持久化 | 支持 | 支持 | 支持 |
| 社区活跃度 | 高 | 极高 | 中 |
| 运维复杂度 | 低 | 中 | 中 |

**推荐：**
- 中小规模：RabbitMQ（运维简单）
- 大规模：Kafka（吞吐量高）
- 阿里系：RocketMQ（延迟低）

### 2. 审计服务模块

```
osys-audit-service/
├── api/                    # API接口层
│   └── AuditQueryApi       # 审计日志查询接口
├── mq/                     # 消息消费
│   ├── AuditLogListener    # 审计日志监听
│   └── RiskEventListener   # 风险事件监听
├── processor/              # 处理器
│   ├── LogProcessor        # 日志处理器
│   ├── RuleEngine          # 规则引擎
│   └── RiskDetector        # 风险检测
├── storage/                # 存储层
│   ├── AuditLogStorage     # 审计日志存储
│   └── RiskEventStorage    # 风险事件存储
├── schedule/               # 定时任务
│   ├── PartitionCreator    # 分区创建
│   └── DataArchive         # 数据归档
└── notify/                 # 通知
    └── RiskAlertNotify     # 风险告警
```

### 3. 消费策略

```java
// 批量消费配置
@RabbitListener(
    queues = "audit-log-queue",
    containerFactory = "batchListenerContainerFactory"
)
public void onAuditLogs(List<AuditMessage> messages) {
    // 1. 批量处理
    List<AuditLog> logs = messages.stream()
        .map(this::convert)
        .collect(Collectors.toList());
    
    // 2. 批量入库
    auditLogStorage.batchSave(logs);
    
    // 3. 异步风险检测
    logs.forEach(log -> riskDetector.asyncDetect(log));
}
```

### 4. 分库分表策略

```yaml
# 按时间分表
sharding:
  tables:
    audit_logs:
      actual-data-nodes: ds0.audit_logs_$->{2024..2030}0$->{1..12}
      table-strategy:
        standard:
          sharding-column: partition_key
          precise-algorithm-class: com.osys.audit.sharding.MonthShardingAlgorithm
```

---

## 业务系统集成

### 1. 发送审计日志（Java SDK）

```java
// 方式1：注解方式（推荐）
@AuditLog(
    eventType = "ORDER_CREATE",
    action = "CREATE",
    description = "创建订单"
)
public Order createOrder(CreateOrderRequest request) {
    // 业务逻辑
}

// 方式2：编程方式
public Order createOrder(CreateOrderRequest request) {
    // 业务逻辑
    Order order = orderService.create(request);
    
    // 发送审计日志
    AuditMessage message = AuditMessage.builder()
        .eventType("ORDER_CREATE")
        .action("CREATE")
        .operator(currentUser())
        .target(Target.of("ORDER", order.getId()))
        .data(Data.of(request, null, order))
        .build();
    
    auditMQSender.send(message);
    
    return order;
}
```

### 2. SDK 自动配置

```yaml
# application.yml
osys:
  audit:
    enabled: true
    app-code: ${spring.application.name}
    mq:
      type: rabbitmq  # rabbitmq/kafka/rocketmq
      exchange: audit.exchange
      routing-key: audit.log
```

---

## 部署架构

```
生产环境部署：

                    ┌─────────────┐
                    │   Nginx     │
                    │  (负载均衡)  │
                    └──────┬──────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼────┐       ┌────▼────┐       ┌────▼────┐
   │ Audit-1 │       │ Audit-2 │       │ Audit-3 │
   └────┬────┘       └────┬────┘       └────┬────┘
        │                  │                  │
        └──────────────────┼──────────────────┘
                           │
                    ┌──────▼──────┐
                    │  RabbitMQ   │
                    │   Cluster   │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
         ┌────▼───┐   ┌────▼───┐   ┌────▼───┐
         │ MySQL  │   │   ES   │   │  OSS   │
         │Primary │   │        │   │        │
         └───┬────┘   └────────┘   └────────┘
             │
         ┌───▼────┐
         │MySQL   │
         │Replica │
         └────────┘
```

---

## 关键特性

### 1. 数据脱敏

```java
@Component
public class DataMasker {
    
    public Object mask(Object data) {
        if (data instanceof String) {
            return maskString((String) data);
        }
        if (data instanceof Map) {
            return maskMap((Map<?, ?>) data);
        }
        return data;
    }
    
    private String maskString(String value) {
        // 手机号: 138****8888
        // 身份证: 110101********1234
        // 银行卡: 6222 **** **** 8888
    }
}
```

### 2. 风险规则引擎

```java
// 规则示例
{
  "ruleCode": "LARGE_PAYMENT",
  "ruleName": "大额支付预警",
  "matchConditions": {
    "eventType": "PAYMENT",
    "requestParams.amount": ">100000"
  },
  "actionType": "ALERT",
  "riskLevel": 2
}
```

### 3. 数据归档

```java
// 3个月前的数据归档到OSS
@Scheduled(cron = "0 0 2 1 * ?") // 每月1号凌晨2点
public void archiveOldData() {
    LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
    
    // 1. 查询旧数据
    List<AuditLog> oldLogs = auditLogRepository.findBefore(threeMonthsAgo);
    
    // 2. 压缩写入OSS
    String archiveFile = compressAndUpload(oldLogs);
    
    // 3. 删除已归档数据
    auditLogRepository.deleteBefore(threeMonthsAgo);
    
    // 4. 记录归档日志
    archiveRecordRepository.save(new ArchiveRecord(archiveFile, oldLogs.size()));
}
```

---

## 监控指标

| 指标 | 说明 | 告警阈值 |
|------|------|---------|
| 消息堆积量 | 待消费消息数 | >10000 |
| 消费延迟 | 消息产生到消费时间 | >30s |
| 消费TPS | 每秒消费数 | 根据业务 |
| 存储使用率 | 磁盘使用率 | >80% |
| 风险事件数 | 检测到的风险数 | 根据业务 |

---

## 项目结构

```
osys-audit-service/
├── src/main/java/com/osys/audit/
│   ├── api/               # REST API
│   ├── config/            # 配置类
│   ├── domain/            # 领域模型
│   ├── infrastructure/    # 基础设施
│   │   ├── mq/            # MQ消费者
│   │   ├── storage/       # 存储实现
│   │   └── notify/        # 通知实现
│   ├── application/       # 应用服务
│   └── sdk/               # 客户端SDK
├── src/main/resources/
│   ├── application.yml
│   └── audit-rules.json   # 默认规则
└── osys-audit-sdk/        # 客户端SDK独立模块
    └── src/main/java/com/osys/audit/sdk/
```

---

## 下一步

需要我创建审计服务的具体代码实现吗？包括：
1. 审计服务主体项目
2. 客户端 SDK（供业务系统使用）
3. MQ 配置和消费者的实现
4. 风险规则引擎