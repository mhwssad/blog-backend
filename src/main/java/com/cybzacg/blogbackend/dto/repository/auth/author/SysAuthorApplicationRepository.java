package com.cybzacg.blogbackend.dto.repository.auth.author;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.auth.SysAuthorApplication;
import com.cybzacg.blogbackend.module.auth.author.model.admin.SysAuthorApplicationAdminPageQuery;

/**
 * 作者申请 Repository。
 *
 * <p>封装作者申请的查询、分页和最近申请读取能力。
 */
public interface SysAuthorApplicationRepository extends IService<SysAuthorApplication> {

    /**
     * 查询用户最近一次提交的作者申请。
     */
    SysAuthorApplication findLatestByUserId(Long userId);

    /**
     * 按用户分页查询作者申请记录。
     */
    Page<SysAuthorApplication> pageByUserId(Long userId, long current, long size);

    /**
     * 按后台条件分页查询作者申请记录。
     */
    Page<SysAuthorApplication> pageByAdminConditions(SysAuthorApplicationAdminPageQuery query);
}
