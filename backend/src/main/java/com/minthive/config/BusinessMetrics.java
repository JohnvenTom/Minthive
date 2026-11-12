package com.minthive.config;

import com.minthive.websocket.WebSocketServer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * 业务指标注册中心
 *
 * <p>功能描述：向 Micrometer 注册 AI 调用耗时、WebSocket 在线连接、审计丢弃与消息推送失败等指标，
 * 由 Actuator 的 /actuator/metrics 暴露给内网监控</p>
 * <p>注意事项：只做指标采集，不承载业务逻辑；Gauge 读取静态在线计数，不持有连接对象</p>
 */
@Component
public class BusinessMetrics {

    private final MeterRegistry registry;

    private final Counter auditDropped;

    private final Counter messagePushFailure;

    private final Timer aiCloud;

    private final Timer aiLocal;

    /**
     * 构造并注册全部指标
     *
     * @param registry Micrometer 注册表
     */
    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.auditDropped = Counter.builder("minthive.audit.dropped")
                .description("审计日志因队列满或落库失败而丢弃的次数")
                .register(registry);
        this.messagePushFailure = Counter.builder("minthive.message.push.failure")
                .description("WebSocket 消息推送失败次数")
                .register(registry);
        this.aiCloud = Timer.builder("minthive.ai.request.duration")
                .description("AI 请求耗时")
                .tag("mode", "cloud")
                .register(registry);
        this.aiLocal = Timer.builder("minthive.ai.request.duration")
                .description("AI 请求耗时")
                .tag("mode", "local")
                .register(registry);
        Gauge.builder("minthive.ws.online", WebSocketServer::onlineCount)
                .description("当前 WebSocket 在线连接数")
                .register(registry);
    }

    /**
     * 记录一次审计丢弃
     */
    public void auditDropped() {
        auditDropped.increment();
    }

    /**
     * 记录一次消息推送失败
     */
    public void messagePushFailed() {
        messagePushFailure.increment();
    }

    /**
     * 开始一次 AI 调用计时
     *
     * @param mode cloud/local
     * @return 计时样本
     */
    public Timer.Sample startAi(String mode) {
        return Timer.start(registry);
    }

    /**
     * 获取对应模式的 AI 计时器
     *
     * @param mode cloud/local
     * @return 计时器
     */
    public Timer aiTimer(String mode) {
        return "local".equalsIgnoreCase(mode) ? aiLocal : aiCloud;
    }
}
