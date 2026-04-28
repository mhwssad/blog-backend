package com.cybzacg.blogbackend.module.auth.service.impl;

import com.cybzacg.blogbackend.common.constant.NoticeConstants;
import com.cybzacg.blogbackend.domain.SysNotice;
import com.cybzacg.blogbackend.domain.SysUserNotice;
import com.cybzacg.blogbackend.enums.auth.NotificationTypeEnum;
import com.cybzacg.blogbackend.module.auth.repository.SysNoticeRepository;
import com.cybzacg.blogbackend.module.auth.repository.SysUserNoticeRepository;
import com.cybzacg.blogbackend.module.auth.service.NotificationDeliveryService;
import com.cybzacg.blogbackend.module.auth.service.UserNotificationPreferenceService;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

/**
 * 用户通知投递服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDeliveryServiceImpl implements NotificationDeliveryService {
    private static final int BUSINESS_NOTICE_TYPE = 1;
    private static final String DEFAULT_LEVEL = "info";
    private static final Long SYSTEM_PUBLISHER_ID = 0L;

    private final SysNoticeRepository sysNoticeRepository;
    private final SysUserNoticeRepository sysUserNoticeRepository;
    private final UserNotificationPreferenceService userNotificationPreferenceService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void deliverAfterCommit(Long targetUserId,
                                   NotificationTypeEnum notificationType,
                                   String title,
                                   String content,
                                   Long publisherId) {
        if (targetUserId == null || notificationType == null || !StrUtils.hasText(title)) {
            return;
        }
        Runnable action = () -> deliver(targetUserId, notificationType, title, content, publisherId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }

    private void deliver(Long targetUserId,
                         NotificationTypeEnum notificationType,
                         String title,
                         String content,
                         Long publisherId) {
        try {
            if (!userNotificationPreferenceService.isNotificationEnabled(targetUserId, notificationType)) {
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            SysNotice notice = buildNotice(targetUserId, title, content, publisherId, now);
            sysNoticeRepository.save(notice);
            sysUserNoticeRepository.save(buildUserNotice(notice.getId(), targetUserId, now));
        } catch (RuntimeException ex) {
            log.warn("deliver user notification failed: targetUserId={}, type={}",
                    targetUserId,
                    notificationType.getCode(),
                    ex);
        }
    }

    private SysNotice buildNotice(Long targetUserId, String title, String content, Long publisherId, LocalDateTime now) {
        Long operatorId = publisherId == null ? SYSTEM_PUBLISHER_ID : publisherId;
        SysNotice notice = new SysNotice();
        notice.setTitle(StrUtils.trim(title));
        notice.setContent(StrUtils.trimToDefault(content, title));
        notice.setType(BUSINESS_NOTICE_TYPE);
        notice.setLevel(DEFAULT_LEVEL);
        notice.setTargetType(NoticeConstants.TARGET_SPECIFIED);
        notice.setTargetUserIds(String.valueOf(targetUserId));
        notice.setPublisherId(operatorId);
        notice.setPublishStatus(NoticeConstants.PUBLISH_STATUS_PUBLISHED);
        notice.setPublishTime(now);
        notice.setCreateBy(operatorId);
        notice.setCreateTime(now);
        notice.setUpdateBy(operatorId);
        notice.setUpdateTime(now);
        notice.setIsDeleted(0);
        return notice;
    }

    private SysUserNotice buildUserNotice(Long noticeId, Long targetUserId, LocalDateTime now) {
        SysUserNotice relation = new SysUserNotice();
        relation.setNoticeId(noticeId);
        relation.setUserId(targetUserId);
        relation.setIsRead(NoticeConstants.READ_UNREAD);
        relation.setCreateTime(now);
        relation.setUpdateTime(now);
        relation.setIsDeleted(0);
        return relation;
    }
}
