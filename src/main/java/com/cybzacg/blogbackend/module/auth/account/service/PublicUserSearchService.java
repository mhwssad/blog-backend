package com.cybzacg.blogbackend.module.auth.account.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.auth.account.model.user.PublicUserSearchVO;

/**
 * 公开用户搜索服务。
 */
public interface PublicUserSearchService {

    /**
     * 按关键词搜索用户，返回分页结果。
     *
     * @param keyword 搜索关键词（至少 2 个字符）
     * @param current 当前页码
     * @param size    每页条数
     * @return 分页用户搜索结果
     */
    PageResult<PublicUserSearchVO> searchUsers(String keyword, long current, long size);
}
