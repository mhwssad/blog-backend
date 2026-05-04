package com.cybzacg.blogbackend.module.report.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.report.model.admin.ReportAdminPageQuery;
import com.cybzacg.blogbackend.module.report.model.admin.ReportAdminVO;
import com.cybzacg.blogbackend.module.report.model.admin.ReportHandleRequest;
import com.cybzacg.blogbackend.module.report.model.common.ReportHandleLogVO;
import java.util.List;

/**
 * 后台举报管理服务接口。
 * <p>
 * 职责：举报记录分页查询、认领、处理、驳回、超管接管、处理日志查看。
 * 所有写操作均在统一事务边界内完成，并记录审计日志。
 */
public interface ReportAdminService {
    /**
     * 分页查询举报记录（管理端）。
     *
     * @param query 查询条件（状态、目标类型、举报人ID、时间范围等）
     * @return 分页结果
     */
    PageResult<ReportAdminVO> pageReports(ReportAdminPageQuery query);

    /**
     * 查询单条举报详情（管理端）。
     *
     * @param reportId 举报记录ID
     * @return 举报详情VO
     */
    ReportAdminVO getReportDetail(Long reportId);

    /**
     * 认领举报。
     * 将待处理的举报标记为处理中，记录当前操作人为处理人。
     *
     * @param reportId   举报记录ID
     * @param operatorId 操作人ID
     */
    void claimReport(Long reportId, Long operatorId);

    /**
     * 处理举报。
     * 更新状态为已处理，写入处理结果与处罚类型，执行业务治理动作并记录审计日志。
     *
     * @param reportId   举报记录ID
     * @param operatorId 操作人ID
     * @param request    处理请求（resultType必填，punishmentType/remark可选）
     * @param ip         请求IP
     * @param ua         UserAgent
     */
    void handleReport(
        Long reportId,
        Long operatorId,
        ReportHandleRequest request,
        String ip,
        String ua
    );

    /**
     * 驳回举报。
     *
     * @param reportId   举报记录ID
     * @param operatorId 操作人ID
     * @param remark     驳回备注
     * @param ip         请求IP
     * @param ua         UserAgent
     */
    void rejectReport(
        Long reportId,
        Long operatorId,
        String remark,
        String ip,
        String ua
    );

    /**
     * 超管接管举报。
     * 仅超级管理员可操作，强占当前处理人并重新认领。
     *
     * @param reportId   举报记录ID
     * @param operatorId 操作人ID（必须为超管）
     * @param ip         请求IP
     * @param ua         UserAgent
     */
    void overrideClaim(Long reportId, Long operatorId, String ip, String ua);

    /**
     * 查询举报关联的处理日志列表。
     *
     * @param reportId 举报记录ID
     * @return 处理日志列表（按创建时间倒序）
     */
    List<ReportHandleLogVO> listHandleLogs(Long reportId);
}
