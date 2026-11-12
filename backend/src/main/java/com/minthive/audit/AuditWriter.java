package com.minthive.audit;

import com.minthive.common.RedisConstants;
import com.minthive.config.BusinessMetrics;
import com.minthive.entity.AuditLogChange;
import com.minthive.mapper.AuditLogChangeMapper;
import com.minthive.mapper.AuditLogMapper;
import com.minthive.util.RedisUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 审计日志异步写入器
 *
 * <p>功能描述：请求结束后把审计事件放入有界队列，由后台线程批量落库；
 * 队列满或落库失败只记录 WARN 并累加 Redis 丢弃计数，绝不影响业务请求</p>
 * <p>注意事项：审计表自身的 Mapper 写入会被 MyBatis 审计拦截器排除，不会递归采集</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditWriter {

    private final AuditLogMapper auditLogMapper;

    private final AuditLogChangeMapper auditLogChangeMapper;

    private final AuditProperties properties;

    private final RedisUtil redisUtil;

    private final BusinessMetrics businessMetrics;

    private final PlatformTransactionManager transactionManager;

    /** 有界队列 */
    private BlockingQueue<AuditEvent> queue;

    /** 是否继续消费 */
    private volatile boolean running = true;

    /** 消费线程 */
    private Thread worker;

    /** 单事件事务模板 */
    private TransactionTemplate transactionTemplate;

    /**
     * 初始化队列与消费线程
     */
    @PostConstruct
    void start() {
        int capacity = Math.max(100, properties.getQueueCapacity());
        queue = new ArrayBlockingQueue<>(capacity);
        transactionTemplate = new TransactionTemplate(transactionManager);
        worker = new Thread(this::consume, "audit-writer");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * 投递一次审计事件
     *
     * @param event 审计事件
     * @note 队列满时丢弃并计数，不阻塞业务线程
     */
    public void submit(AuditEvent event) {
        if (!properties.isEnabled() || event == null) {
            return;
        }
        if (!queue.offer(event)) {
            drop("queue full");
        }
    }

    /**
     * 消费队列并按批落库
     */
    private void consume() {
        while (running || !queue.isEmpty()) {
            try {
                AuditEvent first = queue.poll(500, TimeUnit.MILLISECONDS);
                if (first == null) {
                    continue;
                }
                List<AuditEvent> batch = new ArrayList<>(Math.max(1, properties.getBatchSize()));
                batch.add(first);
                queue.drainTo(batch, Math.max(0, properties.getBatchSize() - 1));
                for (AuditEvent event : batch) {
                    writeOne(event);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                drop("writer loop error: " + e.getMessage());
            }
        }
    }

    /**
     * 单事件落库（主表 + 子表同事务）
     *
     * @param event 审计事件
     */
    private void writeOne(AuditEvent event) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                auditLogMapper.insert(event.log());
                for (AuditLogChange change : event.changes()) {
                    change.setAuditLogId(event.log().getId());
                    auditLogChangeMapper.insert(change);
                }
            });
        } catch (Exception e) {
            drop("write failed: " + e.getMessage());
        }
    }

    /**
     * 记录丢弃/失败并累加 Redis 计数
     *
     * @param reason 原因
     */
    private void drop(String reason) {
        log.warn("审计日志写入失败或丢弃: {}", reason);
        businessMetrics.auditDropped();
        try {
            redisUtil.increment(RedisConstants.AUDIT_DROPPED_PREFIX);
        } catch (Exception e) {
            log.warn("审计丢弃计数写入 Redis 失败: {}", e.getMessage());
        }
    }

    /**
     * 停机时尽力写完剩余事件
     */
    @PreDestroy
    void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt();
            try {
                worker.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (queue != null) {
            List<AuditEvent> rest = new ArrayList<>();
            queue.drainTo(rest);
            for (AuditEvent event : rest) {
                writeOne(event);
            }
        }
    }
}
