package com.cybzacg.blogbackend.module.auth.notice.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.auth.notice.model.admin.SysNoticeAdminVO;
import com.cybzacg.blogbackend.module.auth.notice.model.admin.SysNoticePageQuery;
import com.cybzacg.blogbackend.module.auth.notice.model.admin.SysNoticeSaveRequest;

/**
 * 系统通知后台管理服务接口。
 *
 * <p>定义系统通知后台管理相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface SysNoticeAdminService {
    PageResult<SysNoticeAdminVO> pageNotices(SysNoticePageQuery query);

    SysNoticeAdminVO getNotice(Long id);

    SysNoticeAdminVO createNotice(SysNoticeSaveRequest request);

    SysNoticeAdminVO updateNotice(Long id, SysNoticeSaveRequest request);

    void publishNotice(Long id);

    void revokeNotice(Long id);

    void deleteNotice(Long id);
}
