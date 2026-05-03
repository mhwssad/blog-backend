package com.cybzacg.blogbackend.module.file.task;

import com.cybzacg.blogbackend.module.file.service.FileLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 物理文件删除重试调度器。
 * 定期扫描待物理删除的文件并重试删除。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FilePhysicalDeleteRetryScheduler {
    private final FileLifecycleService fileLifecycleService;

    /**
     * 定时重试物理删除待处理的文件。
     * 扫描标记为待物理删除但尚未成功删除的文件，执行重试删除操作。
     * 仅在有实际处理量时记录 INFO 日志，避免无意义的日志输出。
     */
    @Scheduled(cron = "${file-upload.physical-delete-retry-cron:0 30 * * * *}")
    public void retryPhysicalDelete() {
        int processed = fileLifecycleService.retryPhysicalDeletePendingFiles();
        if (processed > 0) {
            log.info("待物理删除文件重试完成，本次处理文件数: {}", processed);
        }
    }
}
