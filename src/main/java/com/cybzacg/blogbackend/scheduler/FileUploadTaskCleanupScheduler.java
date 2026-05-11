package com.cybzacg.blogbackend.scheduler;

import com.cybzacg.blogbackend.module.file.service.FileLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 文件上传任务清理调度器。
 * 定期清理过期且未完成的上传任务及其临时分片资源。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileUploadTaskCleanupScheduler {
    private final FileLifecycleService fileLifecycleService;

    /**
     * 定时清理过期且未完成的上传任务。
     * 扫描已超过过期时间但尚未完成的上传任务，清理其关联的临时分片资源。
     * 仅在有实际处理量时记录 INFO 日志，避免无意义的日志输出。
     */
    @Scheduled(cron = "${file-upload.expired-task-cleanup-cron:0 0 * * * *}")
    public void cleanupExpiredUploadTasks() {
        int cleaned = fileLifecycleService.cleanupExpiredUploadTasks();
        if (cleaned > 0) {
            log.info("过期上传任务清理完成，本次处理任务数: {}", cleaned);
        }
    }
}
