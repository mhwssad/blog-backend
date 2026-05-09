package com.cybzacg.blogbackend.module.follow.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.follow.model.admin.FollowAdminPageQuery;
import com.cybzacg.blogbackend.module.follow.model.admin.FollowAdminRelationVO;
import com.cybzacg.blogbackend.module.follow.model.admin.FollowRelationCleanRequest;

/**
 * 关注关系后台管理服务。
 *
 * <p>提供运营人员查询与清理关注关系数据的接口。
 */
public interface FollowAdminService {
    /**
     * 按关注/粉丝维度分页查询后台关注关系列表。
     *
     * @param query 查询条件，支持按 followerId、followingId、状态等筛选
     * @return 关注关系分页列表
     */
    PageResult<FollowAdminRelationVO> pageRelations(FollowAdminPageQuery query);

    /**
     * 清理已取关或用户状态异常导致的无效关系，避免后台长期积累脏数据。
     * 事务性删除，清理条件必须至少指定一项。
     *
     * @param request 清理条件请求，包含是否清理已取关、已注销用户、已禁用用户
     * @return 实际清理的记录数
     */
    long cleanRelations(FollowRelationCleanRequest request);
}
