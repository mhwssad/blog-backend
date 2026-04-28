package com.cybzacg.blogbackend.module.dashboard.service.impl;

import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.mapper.DashboardMetricsMapper;
import com.cybzacg.blogbackend.module.dashboard.model.admin.*;
import com.cybzacg.blogbackend.module.dashboard.service.DashboardAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

/**
 * 后台数据看板服务实现。
 */
@Service
@RequiredArgsConstructor
public class DashboardAdminServiceImpl implements DashboardAdminService {
    private static final String RANGE_TODAY = "today";
    private static final String RANGE_WEEK = "week";
    private static final String RANGE_MONTH = "month";
    private static final String RANGE_ALL = "all";
    private static final String RANGE_CUSTOM = "custom";
    private static final long MAX_CUSTOM_RANGE_DAYS = 366L;

    private final DashboardMetricsMapper dashboardMetricsMapper;

    @Override
    public DashboardOverviewVO getOverview(DashboardRangeQuery query) {
        DashboardRange range = resolveRange(query);
        return DashboardOverviewVO.builder()
                .range(range.toVO())
                .registeredUserCount(count(dashboardMetricsMapper.countRegisteredUsers(range.startTime(), range.endTime())))
                .activeUserCount(count(dashboardMetricsMapper.countActiveUsers(range.startTime(), range.endTime())))
                .authorCount(count(dashboardMetricsMapper.countAuthors()))
                .articleCount(count(dashboardMetricsMapper.countArticles(range.startTime(), range.endTime())))
                .pendingArticleReviewCount(count(dashboardMetricsMapper.countPendingArticleReviews()))
                .commentCount(count(dashboardMetricsMapper.countComments(range.startTime(), range.endTime())))
                .chatMessageCount(count(dashboardMetricsMapper.countChatMessages(range.startTime(), range.endTime())))
                .aiCallCount(count(dashboardMetricsMapper.countAiCalls(range.startTime(), range.endTime(), null)))
                .reportCount(count(dashboardMetricsMapper.countReports(range.startTime(), range.endTime(), null)))
                .pendingReportCount(count(dashboardMetricsMapper.countReports(null, null, 0)))
                .build();
    }

    @Override
    public DashboardContentVO getContent(DashboardRangeQuery query) {
        DashboardRange range = resolveRange(query);
        return DashboardContentVO.builder()
                .range(range.toVO())
                .articleCount(count(dashboardMetricsMapper.countArticles(range.startTime(), range.endTime())))
                .pendingArticleReviewCount(count(dashboardMetricsMapper.countPendingArticleReviews()))
                .commentCount(count(dashboardMetricsMapper.countComments(range.startTime(), range.endTime())))
                .likeCount(count(dashboardMetricsMapper.countLikes(range.startTime(), range.endTime())))
                .collectCount(count(dashboardMetricsMapper.countCollections(range.startTime(), range.endTime())))
                .build();
    }

    @Override
    public DashboardCommunityVO getCommunity(DashboardRangeQuery query) {
        DashboardRange range = resolveRange(query);
        return DashboardCommunityVO.builder()
                .range(range.toVO())
                .chatMessageCount(count(dashboardMetricsMapper.countChatMessages(range.startTime(), range.endTime())))
                .lobbyMessageCount(count(dashboardMetricsMapper.countLobbyMessages(range.startTime(), range.endTime())))
                .groupCount(count(dashboardMetricsMapper.countGroups(range.startTime(), range.endTime())))
                .build();
    }

    @Override
    public DashboardAiVO getAi(DashboardRangeQuery query) {
        DashboardRange range = resolveRange(query);
        return DashboardAiVO.builder()
                .range(range.toVO())
                .aiCallCount(count(dashboardMetricsMapper.countAiCalls(range.startTime(), range.endTime(), null)))
                .aiSuccessCallCount(count(dashboardMetricsMapper.countAiCalls(range.startTime(), range.endTime(), 1)))
                .aiFailedCallCount(count(dashboardMetricsMapper.countAiCalls(range.startTime(), range.endTime(), 0)))
                .build();
    }

    @Override
    public DashboardGovernanceVO getGovernance(DashboardRangeQuery query) {
        DashboardRange range = resolveRange(query);
        return DashboardGovernanceVO.builder()
                .range(range.toVO())
                .reportCount(count(dashboardMetricsMapper.countReports(range.startTime(), range.endTime(), null)))
                .pendingReportCount(count(dashboardMetricsMapper.countReports(null, null, 0)))
                .processingReportCount(count(dashboardMetricsMapper.countReports(null, null, 1)))
                .handledReportCount(count(dashboardMetricsMapper.countReports(range.startTime(), range.endTime(), 2)))
                .rejectedReportCount(count(dashboardMetricsMapper.countReports(range.startTime(), range.endTime(), 3)))
                .build();
    }

    private DashboardRange resolveRange(DashboardRangeQuery query) {
        DashboardRangeQuery safeQuery = query == null ? new DashboardRangeQuery() : query;
        String rangeType = StrUtils.trimToDefault(safeQuery.getRangeType(), RANGE_TODAY).toLowerCase(Locale.ROOT);
        LocalDate today = LocalDate.now();
        return switch (rangeType) {
            case RANGE_TODAY -> new DashboardRange(RANGE_TODAY, today.atStartOfDay(), today.plusDays(1).atStartOfDay());
            case RANGE_WEEK -> {
                LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                yield new DashboardRange(RANGE_WEEK, weekStart.atStartOfDay(), weekStart.plusDays(7).atStartOfDay());
            }
            case RANGE_MONTH -> {
                LocalDate monthStart = today.withDayOfMonth(1);
                yield new DashboardRange(RANGE_MONTH, monthStart.atStartOfDay(), monthStart.plusMonths(1).atStartOfDay());
            }
            case RANGE_ALL -> new DashboardRange(RANGE_ALL, null, null);
            case RANGE_CUSTOM -> resolveCustomRange(safeQuery);
            default -> throwIllegalRangeType();
        };
    }

    private DashboardRange resolveCustomRange(DashboardRangeQuery query) {
        LocalDateTime startTime = query.getStartTime();
        LocalDateTime endTime = query.getEndTime();
        ExceptionThrowerCore.throwBusinessIf(startTime == null || endTime == null,
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "自定义时间范围必须同时传入 startTime 和 endTime");
        ExceptionThrowerCore.throwBusinessIf(!startTime.isBefore(endTime),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "开始时间必须早于结束时间");
        ExceptionThrowerCore.throwBusinessIf(Duration.between(startTime, endTime).toDays() > MAX_CUSTOM_RANGE_DAYS,
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "自定义时间范围不能超过366天");
        return new DashboardRange(RANGE_CUSTOM, startTime, endTime);
    }

    private DashboardRange throwIllegalRangeType() {
        ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.ILLEGAL_ARGUMENT, "时间范围类型仅支持 today/week/month/all/custom");
        return null;
    }

    private long count(Long value) {
        return value == null ? 0L : value;
    }

    private record DashboardRange(String rangeType, LocalDateTime startTime, LocalDateTime endTime) {
        private DashboardRangeVO toVO() {
            return DashboardRangeVO.builder()
                    .rangeType(rangeType)
                    .startTime(startTime)
                    .endTime(endTime)
                    .build();
        }
    }
}
