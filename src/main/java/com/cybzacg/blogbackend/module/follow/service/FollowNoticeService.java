package com.cybzacg.blogbackend.module.follow.service;

/**
 * 关注关系通知服务。
 *
 * <p>在关注关系建立后向被关注用户投递新粉丝通知。通知发送在关注主事务提交之后，
 * 保证主链路不受通知失败影响；若当前无活跃事务则同步发送。
 */
public interface FollowNoticeService {
    /**
     * 在关注成功提交后给被关注者投递一条新粉丝通知。
     * <p>该方法注册到关注主事务的 afterCommit 回调中，仅在主事务提交成功后才会发送通知；
     * 若当前无活跃事务则立即发送。通知发送失败仅记录日志，不影响主链路。
     *
     * @param targetUserId 被关注用户ID
     * @param followerUserId 发起关注的新粉丝用户ID
     */
    void notifyNewFollowerAfterCommit(Long targetUserId, Long followerUserId);
}
