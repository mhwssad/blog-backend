package com.cybzacg.blogbackend.module.auth.service;

import com.cybzacg.blogbackend.enums.auth.NotificationTypeEnum;

/**
 * 用户通知投递服务。
 */
public interface NotificationDeliveryService {

    /**
     * 当前事务提交后向指定用户投递通知。
     */
    void deliverAfterCommit(Long targetUserId,
                            NotificationTypeEnum notificationType,
                            String title,
                            String content,
                            Long publisherId);
}
