package com.cybzacg.blogbackend.config.property;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 线程池配置属性。
 *
 * <p>统一承接项目默认异步线程池与定时任务线程池配置，
 * 便于通过 `application.yml` 按环境调整线程池参数。
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "thread-pool")
public class ThreadPoolProperties {
    /**
     * 默认异步线程池配置。
     */
    @Valid
    private Async async = new Async();

    /**
     * 定时任务线程池配置。
     */
    @Valid
    private Scheduled scheduled = new Scheduled();

    @Data
    public static class Async {
        /**
         * 核心线程数；未配置时按 CPU 自动推导。
         */
        @Min(1)
        private Integer corePoolSize;

        /**
         * 最大线程数；未配置时按 CPU 自动推导。
         */
        @Min(1)
        private Integer maxPoolSize;

        /**
         * 队列容量。
         */
        @Min(0)
        private Integer queueCapacity = 256;

        /**
         * 线程名前缀。
         */
        @NotBlank
        private String threadNamePrefix = "async-task-";

        /**
         * 关闭时是否等待任务完成。
         */
        private Boolean waitForTasksToCompleteOnShutdown = true;

        /**
         * 关闭等待秒数。
         */
        @Min(0)
        private Integer awaitTerminationSeconds = 30;

        /**
         * 拒绝策略：caller-runs / abort / discard / discard-oldest
         */
        @NotBlank
        private String rejectionPolicy = "caller-runs";
    }

    @Data
    public static class Scheduled {
        /**
         * 定时线程池大小。
         */
        @Min(1)
        private Integer poolSize = 1;

        /**
         * 线程名前缀。
         */
        @NotBlank
        private String threadNamePrefix = "scheduled-task-";

        /**
         * 拒绝策略：caller-runs / abort / discard / discard-oldest
         */
        @NotBlank
        private String rejectionPolicy = "caller-runs";
    }
}
