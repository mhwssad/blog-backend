package com.cybzacg.blogbackend.module.auth.audit.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.system.SysLog;
import com.cybzacg.blogbackend.module.auth.audit.model.admin.SysLogCleanRequest;
import com.cybzacg.blogbackend.module.auth.audit.model.admin.SysLogPageQuery;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统日志 Repository。
 * <p>封装操作日志实体的持久化操作，提供管理端分页、条件统计及批量清理等能力。
 */
public interface SysLogRepository extends IService<SysLog> {
    /**
     * 根据管理端查询条件对日志进行分页。
     */
    Page<SysLog> pageByAdminConditions(SysLogPageQuery query);

    /**
     * 根据清理条件统计匹配的日志数量。
     */
    long countByConditions(SysLogCleanRequest request);

    /**
     * 根据清理条件删除匹配的日志并返回删除数量。
     */
    long removeByConditions(SysLogCleanRequest request);

    /**
     * 在独立事务中保存一条操作日志。
     * <p>使用 REQUIRES_NEW 传播级别，确保日志写入不受外层事务回滚影响。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    default boolean saveLog(SysLog log) {
        return save(log);
    }
}
