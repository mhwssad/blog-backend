package com.cybzacg.blogbackend.module.chat.task;

import com.cybzacg.blogbackend.module.chat.service.ChatAttachmentAsyncProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 聊天附件异步处理任务调度器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatAttachmentProcessTaskScheduler {
    private final ChatAttachmentAsyncProcessingService chatAttachmentAsyncProcessingService;

    /**
     * 定期回收超时租约并派发到期任务，保证节点重启后仍可继续处理未完成附件。
     */
    @Scheduled(fixedDelayString = "${chat.attachment-processing.dispatch-fixed-delay-ms:15000}")
    public void dispatchPendingTasks() {
        int recovered = chatAttachmentAsyncProcessingService.recoverExpiredTasks();
        if (recovered > 0) {
            log.info("聊天附件异步处理任务已恢复超时租约，本次恢复数: {}", recovered);
        }
        chatAttachmentAsyncProcessingService.dispatchDueTasks();
    }
}
