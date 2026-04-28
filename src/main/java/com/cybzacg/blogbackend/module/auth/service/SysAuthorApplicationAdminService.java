package com.cybzacg.blogbackend.module.auth.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.auth.model.admin.SysAuthorApplicationAdminPageQuery;
import com.cybzacg.blogbackend.module.auth.model.admin.SysAuthorApplicationRepairRequest;
import com.cybzacg.blogbackend.module.auth.model.admin.SysAuthorApplicationAdminReviewRequest;
import com.cybzacg.blogbackend.module.auth.model.admin.SysAuthorApplicationAdminVO;

/**
 * 作者申请后台管理服务。
 */
public interface SysAuthorApplicationAdminService {
    PageResult<SysAuthorApplicationAdminVO> pageApplications(SysAuthorApplicationAdminPageQuery query);

    SysAuthorApplicationAdminVO getApplication(Long id);

    void reviewApplication(Long id, SysAuthorApplicationAdminReviewRequest request);

    void repairApplication(Long id, SysAuthorApplicationRepairRequest request);
}
