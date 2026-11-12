package com.minthive.config;

import com.minthive.audit.AuditScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务执行器配置
 *
 * <p>功能描述：为 @Async 行为日志提供有界线程池，并通过 TaskDecorator 抑制派生写入的审计记录，
 * 避免 GET 浏览触发的日志写入产生无请求上下文的 SYSTEM 审计噪声</p>
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("async-");
        executor.setTaskDecorator(AuditScope::suppress);
        executor.initialize();
        return executor;
    }
}
