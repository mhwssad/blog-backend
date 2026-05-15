package com.cybzacg.blogbackend.module.ai.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.ai.model.admin.AiUsageLogPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiUsageLogVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiUsageStatsVO;

/**
 * AI 使用日志服务接口。
 *
 * <p>负责调用日志记录、分页查询与统计聚合。
 */
public interface AiUsageLogService {

    /**
     * 记录一次 AI 调用日志。
     *
     * @param userId          用户ID
     * @param channelConfigId 渠道配置ID
     * @param sessionId       会话ID
     * @param requestSceneType 请求场景类型
     * @param requestTokens   请求 token 数
     * @param responseTokens  响应 token 数
     * @param totalTokens     总 token 数
     * @param successStatus   成功状态
     * @param errorCode       错误码
     */
    void logUsage(Long userId, Long channelConfigId, Long sessionId,
                  String requestSceneType, Integer requestTokens,
                  Integer responseTokens, Integer totalTokens,
                  Integer successStatus, String errorCode);

    /**
     * 记录一次 AI 调用日志，包含 RAG 检索元数据。
     *
     * @param userId            用户ID
     * @param channelConfigId   渠道配置ID
     * @param sessionId         会话ID
     * @param requestSceneType  请求场景类型
     * @param requestTokens     请求 token 数
     * @param responseTokens    响应 token 数
     * @param totalTokens       总 token 数
     * @param successStatus     成功状态
     * @param errorCode         错误码
     * @param ragEnabled        是否启用 RAG（1-启用，0-未启用）
     * @param ragHitCount       RAG 命中数量
     * @param ragDurationMs     RAG 检索耗时（毫秒）
     * @param ragReferenceJson  RAG 引用来源 JSON
     */
    void logUsage(Long userId, Long channelConfigId, Long sessionId,
                  String requestSceneType, Integer requestTokens,
                  Integer responseTokens, Integer totalTokens,
                  Integer successStatus, String errorCode,
                  Integer ragEnabled, Integer ragHitCount,
                  Long ragDurationMs, String ragReferenceJson);

    /**
     * 分页查询使用日志。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<AiUsageLogVO> pageUsageLogs(AiUsageLogPageQuery query);

    /**
     * 获取使用统计聚合数据。
     *
     * @param query 查询条件（时间范围等）
     * @return 统计信息
     */
    AiUsageStatsVO getUsageStats(AiUsageLogPageQuery query);
}
