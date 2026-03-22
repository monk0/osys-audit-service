package com.osys.audit.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osys.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 审计日志MQ消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = "${audit.mq.queue:audit-log-queue}", containerFactory = "batchRabbitListenerContainerFactory")
public class AuditLogConsumer {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    /**
     * 单条消费
     */
    @RabbitHandler
    public void onMessage(AuditMessage message) {
        try {
            log.debug("收到审计消息: {}", message.getMsgId());
            
            if (!message.isValid()) {
                log.warn("消息验证失败, 丢弃: {}", message.getMsgId());
                return;
            }
            
            auditLogService.process(message);
            
        } catch (Exception e) {
            log.error("处理审计消息失败: {}", message.getMsgId(), e);
            throw e; // 抛出异常触发重试
        }
    }

    /**
     * 批量消费
     */
    @RabbitHandler
    public void onBatchMessage(List<AuditMessage> messages) {
        log.info("批量消费审计消息, 数量: {}", messages.size());
        
        for (AuditMessage message : messages) {
            try {
                if (message.isValid()) {
                    auditLogService.process(message);
                } else {
                    log.warn("批量消息验证失败, 丢弃: {}", message.getMsgId());
                }
            } catch (Exception e) {
                log.error("批量处理审计消息失败: {}", message.getMsgId(), e);
                // 单条失败不影响其他
            }
        }
    }

    /**
     * 原始JSON消费（容错处理）
     */
    @RabbitHandler
    public void onRawMessage(String json) {
        try {
            AuditMessage message = objectMapper.readValue(json, AuditMessage.class);
            onMessage(message);
        } catch (Exception e) {
            log.error("解析审计消息JSON失败: {}", json, e);
        }
    }
}
