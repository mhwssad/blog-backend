package com.cybzacg.blogbackend.module.follow.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.follow.model.admin.FollowAdminPageQuery;
import com.cybzacg.blogbackend.module.follow.model.admin.FollowAdminRelationVO;
import com.cybzacg.blogbackend.module.follow.model.admin.FollowRelationCleanRequest;

/**
 * 关注关系后台管理服务。
 */
public interface FollowAdminService {
    /**
     * 分页查询关注关系。
     */
    PageResult<FollowAdminRelationVO> pageRelations(FollowAdminPageQuery query);

    /**
     * 清理异常关注关系。
     */
    long cleanRelations(FollowRelationCleanRequest request);
}
