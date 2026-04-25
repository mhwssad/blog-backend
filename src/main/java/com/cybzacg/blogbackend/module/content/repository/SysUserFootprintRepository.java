package com.cybzacg.blogbackend.module.content.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.SysUserFootprint;
import com.cybzacg.blogbackend.module.content.model.admin.FootprintPageQuery;

/**
 * 用户足迹 Repository。<p>封装用户浏览足迹的持久化操作，提供后台分页查询、用户足迹分页及 UPSERT 写入。
 */
public interface SysUserFootprintRepository extends IService<SysUserFootprint> {
    /**
     * 按管理端查询条件分页查询足迹。
     *
     * @param query 查询条件
     * @return 足迹分页结果
     */
    Page<SysUserFootprint> pageByAdminConditions(FootprintPageQuery query);

    /**
     * 按用户和目标类型分页查询足迹。
     *
     * @param userId     用户 ID
     * @param targetType 目标类型
     * @param current    当前页
     * @param size       分页大小
     * @return 足迹分页结果
     */
    Page<SysUserFootprint> pageByUserIdAndTargetType(Long userId, String targetType, long current, long size);

    /**
     * 清空指定用户的全部足迹。
     *
     * @param userId 用户 ID
     * @return 是否删除成功
     */
    boolean removeByUserId(Long userId);

    /**
     * 按管理端查询条件批量删除足迹。
     *
     * @param query 查询条件
     * @return 是否删除成功
     */
    boolean removeByAdminConditions(FootprintPageQuery query);

    /**
     * 通过数据库 UPSERT 语义写入最新足迹。
     *
     * @param footprint 足迹实体
     * @return 受影响行数
     */
    int upsertFootprint(SysUserFootprint footprint);

    /**
     * 删除指定目标类型和目标 ID 的全部足迹。
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @return 是否删除成功
     */
    boolean removeByTargetTypeAndTargetId(String targetType, Long targetId);
}
