package com.cybzacg.blogbackend.module.auth.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.auth.model.admin.UserNoticePageQuery;
import com.cybzacg.blogbackend.module.auth.model.admin.UserNoticeVO;

/**
 * 用户通知Inbox服务接口。
 *
 * <p>定义用户通知Inbox相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface UserNoticeInboxService {
    PageResult<UserNoticeVO> pageMyNotices(UserNoticePageQuery query);

    UserNoticeVO getMyNotice(Long noticeId);

    long countUnreadNotices();

    void markRead(Long noticeId);

    void markAllRead();
}
