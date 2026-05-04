package com.cybzacg.blogbackend.module.report.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.report.model.user.ReportCreateRequest;
import com.cybzacg.blogbackend.module.report.model.user.ReportVO;

/**
 * 用户侧举报服务接口。
 * <p>
 * 职责：接收用户举报申请、查询个人举报记录与详情。
 * 业务约束：同一用户对同一目标24小时内只允许举报一次。
 */
public interface ReportService {
    /**
     * 提交举报。
     *
     * @param userId   举报人ID
     * @param request  举报请求（targetType, targetId, reason可选）
     * @return 举报记录VO
     * @throws BusinessException 目标类型非法或存在重复举报时抛出
     */
    ReportVO submitReport(Long userId, ReportCreateRequest request);

    /**
     * 分页查询当前用户的举报记录列表。
     *
     * @param userId     用户ID
     * @param targetType 目标类型筛选（可选）
     * @param current   页码
     * @param size      每页记录数
     * @return 分页结果
     */
    PageResult<ReportVO> listMyReports(
        Long userId,
        String targetType,
        long current,
        long size
    );

    /**
     * 查询当前用户的单条举报详情。
     *
     * @param userId   用户ID
     * @param reportId 举报记录ID
     * @return 举报记录VO
     * @throws BusinessException 记录不存在或不属于该用户时抛出
     */
    ReportVO getMyReport(Long userId, Long reportId);
}
