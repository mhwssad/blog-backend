package com.cybzacg.blogbackend.module.auth.notice.service.impl;

import com.cybzacg.blogbackend.common.constant.NoticeConstants;
import com.cybzacg.blogbackend.domain.notice.SysUserNotice;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 通知相关对象工厂方法，收口 SysUserNotice 的创建逻辑。
 */
@Component
public class SysNoticeFactory {

    /**
     * 构建通知投递记录（发送通知时使用）。
     */
    public SysUserNotice createDeliveryRecord(Long noticeId, Long userId, LocalDateTime now) {
        SysUserNotice record = new SysUserNotice();
        record.setNoticeId(noticeId);
        record.setUserId(userId);
        record.setIsRead(NoticeConstants.READ_UNREAD);
        record.setReadTime(null);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setIsDeleted(0);
        return record;
    }

    /**
     * 构建通知已读记录（标记已读时使用）。
     */
    public SysUserNotice createReadRecord(Long noticeId, Long userId, LocalDateTime now) {
        SysUserNotice record = new SysUserNotice();
        record.setNoticeId(noticeId);
        record.setUserId(userId);
        record.setIsRead(NoticeConstants.READ_READ);
        record.setReadTime(now);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setIsDeleted(0);
        return record;
    }
}
