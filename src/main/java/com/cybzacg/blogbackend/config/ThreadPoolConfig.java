package com.cybzacg.blogbackend.config;

import com.cybzacg.blogbackend.config.property.ThreadPoolProperties;
import com.cybzacg.blogbackend.utils.StrUtils;
import com.cybzacg.blogbackend.utils.TransmittableContextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 统一线程池配置。<p>集中管理项目默认异步线程池和定时任务线程池，同时作为 {@code @Async} 的默认执行器，并通过 TTL 包装透传请求上下文。</p>
 */
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class ThreadPoolConfig implements AsyncConfigurer {
    private final ThreadPoolProperties threadPoolProperties;

    private static ThreadFactory namedDaemonThreadFactory(String prefix) {
        AtomicInteger idx = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(prefix + idx.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }

    /**
     * 创建项目默认异步线程池，同时保留 shortTaskExecutor 别名以兼容既有注入点。
     * <p>未配置的线程数参数将根据 CPU 核心数自动推导。</p>
     *
     * @return ThreadPoolTaskExecutor 实例
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
     * 创建项目统一定时任务线程池。
     *
     * @return ScheduledExecutorService 实例
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

    /**
     * 返回 {@code @Async} 使用的默认异步执行器。
     *
     * @return 异步线程池执行器
     */
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
     *
     * @param rejectionPolicy 拒绝策略名称
     * @return 对应的 RejectedExecutionHandler 实例
     */
    private RejectedExecutionHandler resolveRejectedExecutionHandler(String rejectionPolicy) {
        String normalizedPolicy = StrUtils.nullToEmpty(StrUtils.trimToLowerCase(rejectionPolicy));
        return switch (normalizedPolicy) {
            case "caller-runs", "caller_runs", "callerruns" -> callerRunsPolicy();
            case "discard" -> discardPolicy();
            case "discard-oldest", "discard_oldest", "discardoldest" -> discardOldestPolicy();
            case "abort", "" -> abortPolicy();
            default -> callerRunsPolicy();
        };
    }
}
