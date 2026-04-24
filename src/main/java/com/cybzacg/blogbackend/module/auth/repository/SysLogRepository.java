package com.cybzacg.blogbackend.module.auth.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysLog;
import com.cybzacg.blogbackend.module.auth.model.admin.SysLogCleanRequest;
import com.cybzacg.blogbackend.module.auth.model.admin.SysLogPageQuery;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统日志 Repository。
 */
public interface SysLogRepository extends IService<SysLog> {
    Page<SysLog> pageByAdminConditions(SysLogPageQuery query);

    long countByConditions(SysLogCleanRequest request);

    long removeByConditions(SysLogCleanRequest request);

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    default boolean saveLog(SysLog log) {
        return save(log);
    }
}
