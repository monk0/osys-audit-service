# OSYS Audit Service

OSYS 审计服务 - 基于 RabbitMQ 的独立审计系统

## 概述

本服务是 OSYS 平台的审计模块，负责：
- 接收并处理审计日志消息
- 风险事件检测与记录
- 审计规则管理

## 技术栈

| 技术 | 说明 |
|------|------|
| Spring Boot 3.2 | 基础框架 |
| RabbitMQ | 消息队列 |
| MyBatis Plus | ORM 框架 |
| MySQL 8 | 数据存储 |
| Druid | 连接池 |

## 项目结构

```
osys-audit-service/
├── src/
│   ├── main/
│   │   ├── java/com/osys/audit/
│   │   │   ├── AuditServiceApplication.java
│   │   │   ├── config/          # 配置类
│   │   │   ├── controller/      # API 控制器
│   │   │   ├── entity/          # 实体类
│   │   │   ├── mapper/          # MyBatis Mapper
│   │   │   ├── mq/              # MQ 消费者
│   │   │   ├── processor/       # 风险处理器
│   │   │   └── service/         # 业务服务
│   │   └── resources/
│   │       ├── application.yml  # 主配置
│   │       └── mapper/          # XML 映射文件
│   └── test/                    # 测试代码
├── pom.xml                      # Maven 配置
├── schema.sql                   # 数据库脚本
└── README.md                    # 本文档
```

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- RabbitMQ 3.10+

### 2. 数据库初始化

```bash
mysql -u root -p < schema.sql
```

### 3. 配置文件

修改 `application.yml` 中的数据库和 RabbitMQ 连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/osys_audit
    username: your_username
    password: your_password
  
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

### 4. 构建与运行

```bash
# 编译
mvn clean compile

# 运行
mvn spring-boot:run

# 或使用开发模式
mvn spring-boot:run -Pdev
```

## 构建命令

```bash
# 开发环境（默认，跳过测试）
mvn clean package

# 测试环境
mvn clean package -Ptest

# 生产环境
mvn clean package -Pprod

# 运行测试
mvn clean test

# 生成覆盖率报告
mvn clean test jacoco:report
```

## 父项目

本服务继承自 [osys-parent](https://github.com/monk0/osys-parent)，统一版本管理。

```xml
<parent>
    <groupId>com.osys</groupId>
    <artifactId>osys-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>
```

## API 文档

启动后访问：
- Swagger UI: `http://localhost:9001/swagger-ui.html`
- API Docs: `http://localhost:9001/v3/api-docs`

## 配置说明

### 端口

默认端口：`9001`

### MQ 配置

```yaml
audit:
  mq:
    exchange: audit.exchange
    queue: audit-log-queue
    routing-key: audit.log
```

## 许可证

MIT
