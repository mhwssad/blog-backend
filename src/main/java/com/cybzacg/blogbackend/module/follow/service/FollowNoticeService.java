package com.cybzacg.blogbackend.module.follow.service;

/**
 * 关注关系通知服务。
 */
public interface FollowNoticeService {
    /**
     * 在关注成功提交后给被关注者投递一条新粉丝通知。
     */
    void notifyNewFollowerAfterCommit(Long targetUserId, Long followerUserId);
}
