package com.cybzacg.blogbackend.config;

import com.cybzacg.blogbackend.config.property.ThreadPoolProperties;
import com.cybzacg.blogbackend.utils.TransmittableContextUtils;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 统一线程池配置。
 *
 * <p>集中收口项目内默认异步执行器和定时执行器，避免各模块零散创建线程池。
 * 当前默认异步线程池同时作为 `@Async` 的默认执行器，并通过 TTL 包装透传请求上下文。
 */
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class ThreadPoolConfig implements AsyncConfigurer {
    private final ThreadPoolProperties threadPoolProperties;

    /**
     * 项目默认异步线程池。
     *
     * <p>同时保留 `shortTaskExecutor` 别名，兼容既有注入点平滑迁移。
     */
    @Bean(name = {"asyncTaskExecutor", "shortTaskExecutor"})
    @ConditionalOnMissingBean(name = "asyncTaskExecutor")
    public ThreadPoolTaskExecutor asyncTaskExecutor() {
        int cpu = Runtime.getRuntime().availableProcessors();
        ThreadPoolProperties.Async properties = threadPoolProperties.getAsync();
        int corePoolSize = properties.getCorePoolSize() != null ? properties.getCorePoolSize() : Math.max(2, cpu);
        int maxPoolSize = properties.getMaxPoolSize() != null ? properties.getMaxPoolSize() : Math.max(4, cpu * 2);
        maxPoolSize = Math.max(corePoolSize, maxPoolSize);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        executor.setTaskDecorator(TransmittableContextUtils::wrap);
        executor.setRejectedExecutionHandler(resolveRejectedExecutionHandler(properties.getRejectionPolicy()));
        executor.setWaitForTasksToCompleteOnShutdown(Boolean.TRUE.equals(properties.getWaitForTasksToCompleteOnShutdown()));
        executor.setAwaitTerminationSeconds(properties.getAwaitTerminationSeconds());
        executor.initialize();
        return executor;
    }

    /**
     * 项目统一定时任务线程池。
     */
    @Bean(name = "scheduledTaskExecutor", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "scheduledTaskExecutor")
    public ScheduledExecutorService scheduledTaskExecutor() {
        ThreadPoolProperties.Scheduled properties = threadPoolProperties.getScheduled();
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
                properties.getPoolSize(),
                namedDaemonThreadFactory(properties.getThreadNamePrefix()),
                resolveRejectedExecutionHandler(properties.getRejectionPolicy())
        );
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return asyncTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return AsyncConfigurer.super.getAsyncUncaughtExceptionHandler();
    }

    private ThreadPoolExecutor.AbortPolicy abortPolicy() {
        return new ThreadPoolExecutor.AbortPolicy();
    }

    private ThreadPoolExecutor.CallerRunsPolicy callerRunsPolicy() {
        return new ThreadPoolExecutor.CallerRunsPolicy();
    }

    private ThreadPoolExecutor.DiscardPolicy discardPolicy() {
        return new ThreadPoolExecutor.DiscardPolicy();
    }

    private ThreadPoolExecutor.DiscardOldestPolicy discardOldestPolicy() {
        return new ThreadPoolExecutor.DiscardOldestPolicy();
    }

    /**
     * 将配置文件中的拒绝策略字符串映射为 JDK 线程池拒绝策略。
     */
    private RejectedExecutionHandler resolveRejectedExecutionHandler(String rejectionPolicy) {
        String normalizedPolicy = rejectionPolicy == null ? "" : rejectionPolicy.trim().toLowerCase();
        return switch (normalizedPolicy) {
            case "caller-runs", "caller_runs", "callerruns" -> callerRunsPolicy();
            case "discard" -> discardPolicy();
            case "discard-oldest", "discard_oldest", "discardoldest" -> discardOldestPolicy();
            case "abort", "" -> abortPolicy();
            default -> callerRunsPolicy();
        };
    }

    private static ThreadFactory namedDaemonThreadFactory(String prefix) {
        AtomicInteger idx = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(prefix + idx.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }
}
