package com.cybzacg.blogbackend.module.auth.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.auth.model.admin.SysLogAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysLogCleanRequest;
import com.cybzacg.blogbackend.module.auth.model.admin.SysLogPageQuery;

/**
 * 系统日志后台管理服务接口。
 *
 * <p>定义系统日志后台管理相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface SysLogAdminService {
    PageResult<SysLogAdminVO> pageLogs(SysLogPageQuery query);

    SysLogAdminVO getLog(Long id);

    void deleteLog(Long id);

    long cleanLogs(SysLogCleanRequest request);
}
