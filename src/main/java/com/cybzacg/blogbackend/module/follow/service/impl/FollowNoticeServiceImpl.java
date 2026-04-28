package com.cybzacg.blogbackend.module.follow.service.impl;

import com.cybzacg.blogbackend.common.constant.NoticeConstants;
import com.cybzacg.blogbackend.domain.SysNotice;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.domain.SysUserNotice;
import com.cybzacg.blogbackend.enums.auth.NotificationTypeEnum;
import com.cybzacg.blogbackend.module.auth.repository.SysNoticeRepository;
import com.cybzacg.blogbackend.module.auth.repository.SysUserNoticeRepository;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.service.UserNotificationPreferenceService;
import com.cybzacg.blogbackend.module.follow.service.FollowNoticeService;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 关注关系通知服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowNoticeServiceImpl implements FollowNoticeService {
    private static final int FOLLOW_NOTICE_TYPE = 1;
    private static final String FOLLOW_NOTICE_LEVEL = "info";
    private static final String FOLLOW_NOTICE_TITLE = "你收到了一位新粉丝";

    private final SysNoticeRepository sysNoticeRepository;
    private final SysUserNoticeRepository sysUserNoticeRepository;
    private final SysUserRepository sysUserRepository;
    private final UserNotificationPreferenceService userNotificationPreferenceService;

    /**
     * 关注主事务提交成功后再投递通知，避免通知失败反向影响关注主链路。
     */
    @Override
    public void notifyNewFollowerAfterCommit(Long targetUserId, Long followerUserId) {
        if (targetUserId == null || followerUserId == null || Objects.equals(targetUserId, followerUserId)) {
            return;
        }
        Runnable action = () -> createNewFollowerNotice(targetUserId, followerUserId);
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

    private void createNewFollowerNotice(Long targetUserId, Long followerUserId) {
        if (!userNotificationPreferenceService.isNotificationEnabled(targetUserId, NotificationTypeEnum.FOLLOW_ME)) {
            return;
        }
        SysUser follower = sysUserRepository.getById(followerUserId);
        if (follower == null || !Objects.equals(follower.getDeletedFlag(), 0)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String displayName = StrUtils.hasText(follower.getNickname()) ? follower.getNickname().trim()
                : StrUtils.trimToDefault(follower.getUsername(), "有新用户");

        SysNotice notice = buildFollowNotice(targetUserId, followerUserId, displayName, now);
        try {
            sysNoticeRepository.save(notice);
            sysUserNoticeRepository.save(buildFollowUserNotice(notice.getId(), targetUserId, now));
        } catch (RuntimeException ex) {
            log.warn("create follow notice failed: followerUserId={}, targetUserId={}", followerUserId, targetUserId, ex);
        }
    }

    private SysNotice buildFollowNotice(Long targetUserId, Long followerUserId, String displayName, LocalDateTime now) {
        SysNotice notice = new SysNotice();
        notice.setTitle(FOLLOW_NOTICE_TITLE);
        notice.setContent(displayName + " 关注了你");
        notice.setType(FOLLOW_NOTICE_TYPE);
        notice.setLevel(FOLLOW_NOTICE_LEVEL);
        notice.setTargetType(NoticeConstants.TARGET_SPECIFIED);
        notice.setTargetUserIds(String.valueOf(targetUserId));
        notice.setPublisherId(followerUserId);
        notice.setPublishStatus(NoticeConstants.PUBLISH_STATUS_PUBLISHED);
        notice.setPublishTime(now);
        notice.setCreateBy(followerUserId);
        notice.setCreateTime(now);
        notice.setUpdateBy(followerUserId);
        notice.setUpdateTime(now);
        notice.setIsDeleted(0);
        return notice;
    }

    private SysUserNotice buildFollowUserNotice(Long noticeId, Long targetUserId, LocalDateTime now) {
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
