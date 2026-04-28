package com.cybzacg.blogbackend.module.ai.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.AiUsageLog;
import com.cybzacg.blogbackend.enums.ai.AiUsageSuccessStatusEnum;
import com.cybzacg.blogbackend.module.ai.convert.AiModelMapper;
import com.cybzacg.blogbackend.module.ai.model.admin.AiUsageLogPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiUsageLogVO;
import com.cybzacg.blogbackend.module.ai.model.admin.AiUsageStatsVO;
import com.cybzacg.blogbackend.module.ai.repository.AiUsageLogRepository;
import com.cybzacg.blogbackend.module.ai.service.AiUsageLogService;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 使用日志服务实现。
 *
 * <p>负责调用日志的持久化记录、分页查询与统计聚合。
 */
@Service
@RequiredArgsConstructor
public class AiUsageLogServiceImpl implements AiUsageLogService {

    private final AiUsageLogRepository aiUsageLogRepository;
    private final AiModelMapper aiModelMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public void logUsage(Long userId, Long channelConfigId, Long sessionId,
                         String requestSceneType, Integer requestTokens,
                         Integer responseTokens, Integer totalTokens,
                         Integer successStatus, String errorCode) {
        AiUsageLog log = new AiUsageLog();
        log.setUserId(userId);
        log.setChannelConfigId(channelConfigId);
        log.setSessionId(sessionId);
        log.setRequestSceneType(requestSceneType);
        log.setRequestTokens(requestTokens);
        log.setResponseTokens(responseTokens);
        log.setTotalTokens(totalTokens);
        log.setQuotaCost(totalTokens != null ? totalTokens : 0);
        log.setSuccessStatus(successStatus);
        log.setErrorCode(errorCode);
        aiUsageLogRepository.save(log);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<AiUsageLogVO> pageUsageLogs(AiUsageLogPageQuery query) {
        long current = PaginationUtils.normalizeCurrent(query.getCurrent());
        long size = PaginationUtils.normalizeSize(query.getSize(), 20L, 100L);

        Page<AiUsageLog> page = aiUsageLogRepository.pageByQuery(
                query.getUserId(),
                query.getChannelConfigId(),
                query.getStartTime(),
                query.getEndTime(),
                query.getSuccessStatus(),
                current,
                size);

        List<AiUsageLogVO> records = page.getRecords().stream()
                .map(aiModelMapper::toUsageLogVO)
                .toList();

        return PageResult.of(page, records);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AiUsageStatsVO getUsageStats(AiUsageLogPageQuery query) {
        LocalDateTime startTime = query.getStartTime();
        LocalDateTime endTime = query.getEndTime();

        long totalCalls = aiUsageLogRepository.countByCreatedAtBetweenAndSuccessStatus(startTime, endTime, null);
        long successCalls = aiUsageLogRepository.countByCreatedAtBetweenAndSuccessStatus(
                startTime, endTime, AiUsageSuccessStatusEnum.SUCCESS.getValue());
        long failedCalls = totalCalls - successCalls;
        long totalTokens = aiUsageLogRepository.sumTotalTokensByCreatedAtBetween(startTime, endTime);
        long totalQuotaCost = aiUsageLogRepository.sumQuotaCostByCreatedAtBetween(startTime, endTime);

        AiUsageStatsVO stats = new AiUsageStatsVO();
        stats.setTotalCalls(totalCalls);
        stats.setSuccessCalls(successCalls);
        stats.setFailedCalls(failedCalls);
        stats.setTotalTokens(totalTokens);
        stats.setTotalQuotaCost(totalQuotaCost);
        return stats;
    }
}
