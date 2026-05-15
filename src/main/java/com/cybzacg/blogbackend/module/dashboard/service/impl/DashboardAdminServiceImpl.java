package com.cybzacg.blogbackend.module.dashboard.service.impl;

import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcel;
import cn.idev.excel.write.metadata.WriteSheet;
import com.cybzacg.blogbackend.common.constant.RedisConstants;
import com.cybzacg.blogbackend.common.redis.RedisOperator;
import com.cybzacg.blogbackend.dto.mapper.dashboard.DashboardMetricsMapper;
import com.cybzacg.blogbackend.module.dashboard.model.admin.*;import com.cybzacg.blogbackend.module.dashboard.service.DashboardAdminService;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * 后台数据看板服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardAdminServiceImpl implements DashboardAdminService {
    private static final String RANGE_TODAY = "today";
    private static final String RANGE_WEEK = "week";
    private static final String RANGE_MONTH = "month";
    private static final String RANGE_ALL = "all";
    private static final String RANGE_CUSTOM = "custom";
    private static final int HOT_SECTION_LIMIT = 5;
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final DashboardMetricsMapper dashboardMetricsMapper;
    private final RedisOperator redisOperator;

    @Override
    public DashboardOverviewVO getOverview(DashboardRangeQuery query) {
        DashboardRange range = resolveRange(query);
        return cacheOrCompute("overview", range, DashboardOverviewVO.class, () -> DashboardOverviewVO.builder()
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
                .build());
    }

    @Override
    public DashboardContentVO getContent(DashboardRangeQuery query) {
        DashboardRange range = resolveRange(query);
        return cacheOrCompute("content", range, DashboardContentVO.class, () -> DashboardContentVO.builder()
                .range(range.toVO())
                .articleCount(count(dashboardMetricsMapper.countArticles(range.startTime(), range.endTime())))
                .pendingArticleReviewCount(count(dashboardMetricsMapper.countPendingArticleReviews()))
                .commentCount(count(dashboardMetricsMapper.countComments(range.startTime(), range.endTime())))
                .likeCount(count(dashboardMetricsMapper.countLikes(range.startTime(), range.endTime())))
                .collectCount(count(dashboardMetricsMapper.countCollections(range.startTime(), range.endTime())))
                .build());
    }

    @Override
    public DashboardCommunityVO getCommunity(DashboardRangeQuery query) {
        DashboardRange range = resolveRange(query);
        return cacheOrCompute("community", range, DashboardCommunityVO.class, () -> DashboardCommunityVO.builder()
                .range(range.toVO())
                .chatMessageCount(count(dashboardMetricsMapper.countChatMessages(range.startTime(), range.endTime())))
                .lobbyMessageCount(count(dashboardMetricsMapper.countLobbyMessages(range.startTime(), range.endTime())))
                .groupCount(count(dashboardMetricsMapper.countGroups(range.startTime(), range.endTime())))
                .forumPostCount(count(dashboardMetricsMapper.countForumPosts(range.startTime(), range.endTime())))
                .forumReplyCount(count(dashboardMetricsMapper.countForumReplies(range.startTime(), range.endTime())))
                .hotSections(dashboardMetricsMapper.listHotSections(range.startTime(), range.endTime(), HOT_SECTION_LIMIT))
                .build());
    }

    @Override
    public DashboardAiVO getAi(DashboardRangeQuery query) {
        DashboardRange range = resolveRange(query);
        return cacheOrCompute("ai", range, DashboardAiVO.class, () -> DashboardAiVO.builder()
                .range(range.toVO())
                .aiCallCount(count(dashboardMetricsMapper.countAiCalls(range.startTime(), range.endTime(), null)))
                .aiSuccessCallCount(count(dashboardMetricsMapper.countAiCalls(range.startTime(), range.endTime(), 1)))
                .aiFailedCallCount(count(dashboardMetricsMapper.countAiCalls(range.startTime(), range.endTime(), 0)))
                .ragCallCount(count(dashboardMetricsMapper.countRagCalls(range.startTime(), range.endTime())))
                .agentTaskCount(count(dashboardMetricsMapper.countAgentTasks(range.startTime(), range.endTime(), null)))
                .agentSuccessTaskCount(count(dashboardMetricsMapper.countAgentTasks(range.startTime(), range.endTime(), 2)))
                .agentFailedTaskCount(count(dashboardMetricsMapper.countAgentTasks(range.startTime(), range.endTime(), 3)))
                .build());
    }

    @Override
    public DashboardGovernanceVO getGovernance(DashboardRangeQuery query) {
        DashboardRange range = resolveRange(query);
        return cacheOrCompute("governance", range, DashboardGovernanceVO.class, () -> DashboardGovernanceVO.builder()
                .range(range.toVO())
                .reportCount(count(dashboardMetricsMapper.countReports(range.startTime(), range.endTime(), null)))
                .pendingReportCount(count(dashboardMetricsMapper.countReports(null, null, 0)))
                .processingReportCount(count(dashboardMetricsMapper.countReports(null, null, 1)))
                .handledReportCount(count(dashboardMetricsMapper.countReports(range.startTime(), range.endTime(), 2)))
                .rejectedReportCount(count(dashboardMetricsMapper.countReports(range.startTime(), range.endTime(), 3)))
                .averageHandleDurationMinutes(defaultDecimal(
                        dashboardMetricsMapper.averageReportHandleDurationMinutes(range.startTime(), range.endTime())))
                .punishmentDistributions(dashboardMetricsMapper.listPunishmentDistributions(range.startTime(), range.endTime()))
                .build());
    }

    @Override
    public byte[] exportDashboard(DashboardRangeQuery query) {
        DashboardOverviewVO overview = getOverview(query);
        DashboardContentVO content = getContent(query);
        DashboardCommunityVO community = getCommunity(query);
        DashboardAiVO ai = getAi(query);
        DashboardGovernanceVO governance = getGovernance(query);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ExcelWriter excelWriter = FastExcel.write(outputStream).autoCloseStream(false).build()) {
            writeSheet(excelWriter, 0, "概览", List.of("指标", "数值"), overviewRows(overview));
            writeSheet(excelWriter, 1, "内容", List.of("指标", "数值"), contentRows(content));
            writeSheet(excelWriter, 2, "社区", List.of("指标", "数值"), communityRows(community));
            writeSheet(excelWriter, 3, "AI", List.of("指标", "数值"), aiRows(ai));
            writeSheet(excelWriter, 4, "治理", List.of("指标", "数值"), governanceRows(governance));
            writeSheet(excelWriter, 5, "热门版块", List.of("版块ID", "版块名称", "发帖数", "回复数", "热度值"),
                    hotSectionRows(community.getHotSections()));
            writeSheet(excelWriter, 6, "处罚分布", List.of("处罚类型", "数量"),
                    punishmentRows(governance.getPunishmentDistributions()));
            excelWriter.finish();
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("导出运营看板统计失败", ex);
        }
    }

    /**
     * 将当前看板聚合结果写入 Excel，避免导出接口重复拼装统计口径。
     */
    private void writeSheet(ExcelWriter excelWriter, int sheetNo, String sheetName,
                            List<String> head, List<List<Object>> rows) {
        WriteSheet sheet = FastExcel.writerSheet(sheetNo, sheetName)
                .head(head.stream().map(List::of).toList())
                .build();
        excelWriter.write(rows, sheet);
    }

    private List<List<Object>> overviewRows(DashboardOverviewVO overview) {
        List<List<Object>> rows = baseRangeRows(overview.getRange());
        rows.add(row("注册用户数", overview.getRegisteredUserCount()));
        rows.add(row("活跃用户数", overview.getActiveUserCount()));
        rows.add(row("作者数量", overview.getAuthorCount()));
        rows.add(row("文章总数", overview.getArticleCount()));
        rows.add(row("待审核文章数", overview.getPendingArticleReviewCount()));
        rows.add(row("评论数", overview.getCommentCount()));
        rows.add(row("私信消息数", overview.getChatMessageCount()));
        rows.add(row("AI 调用次数", overview.getAiCallCount()));
        rows.add(row("举报总数", overview.getReportCount()));
        rows.add(row("待处理举报数", overview.getPendingReportCount()));
        return rows;
    }

    private List<List<Object>> contentRows(DashboardContentVO content) {
        List<List<Object>> rows = baseRangeRows(content.getRange());
        rows.add(row("文章总数", content.getArticleCount()));
        rows.add(row("待审核文章数", content.getPendingArticleReviewCount()));
        rows.add(row("评论数", content.getCommentCount()));
        rows.add(row("点赞数", content.getLikeCount()));
        rows.add(row("收藏数", content.getCollectCount()));
        return rows;
    }

    private List<List<Object>> communityRows(DashboardCommunityVO community) {
        List<List<Object>> rows = baseRangeRows(community.getRange());
        rows.add(row("私信消息数", community.getChatMessageCount()));
        rows.add(row("大厅消息数", community.getLobbyMessageCount()));
        rows.add(row("群组数量", community.getGroupCount()));
        rows.add(row("论坛发帖数", community.getForumPostCount()));
        rows.add(row("论坛回复数", community.getForumReplyCount()));
        return rows;
    }

    private List<List<Object>> aiRows(DashboardAiVO ai) {
        List<List<Object>> rows = baseRangeRows(ai.getRange());
        rows.add(row("AI 调用总次数", ai.getAiCallCount()));
        rows.add(row("AI 成功调用次数", ai.getAiSuccessCallCount()));
        rows.add(row("AI 失败调用次数", ai.getAiFailedCallCount()));
        rows.add(row("RAG 调用次数", ai.getRagCallCount()));
        rows.add(row("Agent 任务总数", ai.getAgentTaskCount()));
        rows.add(row("Agent 成功任务数", ai.getAgentSuccessTaskCount()));
        rows.add(row("Agent 失败任务数", ai.getAgentFailedTaskCount()));
        return rows;
    }

    private List<List<Object>> governanceRows(DashboardGovernanceVO governance) {
        List<List<Object>> rows = baseRangeRows(governance.getRange());
        rows.add(row("举报总数", governance.getReportCount()));
        rows.add(row("待处理举报数", governance.getPendingReportCount()));
        rows.add(row("处理中举报数", governance.getProcessingReportCount()));
        rows.add(row("已处理举报数", governance.getHandledReportCount()));
        rows.add(row("已驳回举报数", governance.getRejectedReportCount()));
        rows.add(row("平均举报处理耗时（分钟）", governance.getAverageHandleDurationMinutes()));
        return rows;
    }

    private List<List<Object>> hotSectionRows(List<DashboardHotSectionVO> hotSections) {
        if (hotSections == null || hotSections.isEmpty()) {
            return List.of();
        }
        return hotSections.stream()
                .map(item -> List.<Object>of(
                        item.getSectionId(),
                        item.getSectionName(),
                        count(item.getPostCount()),
                        count(item.getReplyCount()),
                        count(item.getHotValue())))
                .toList();
    }

    private List<List<Object>> punishmentRows(List<DashboardPunishmentDistributionVO> distributions) {
        if (distributions == null || distributions.isEmpty()) {
            return List.of();
        }
        return distributions.stream()
                .map(item -> List.<Object>of(item.getPunishmentType(), count(item.getCount())))
                .toList();
    }

    private List<List<Object>> baseRangeRows(DashboardRangeVO range) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(row("时间范围", range.getRangeType()));
        rows.add(row("统计开始时间", range.getStartTime()));
        rows.add(row("统计结束时间", range.getEndTime()));
        return rows;
    }

    private List<Object> row(String name, Object value) {
        return List.of(name, value == null ? "" : value);
    }

    private <T> T cacheOrCompute(String type, DashboardRange range, Class<T> voClass, java.util.function.Supplier<T> supplier) {
        String cacheKey = buildCacheKey(type, range);
        try {
            Object cached = redisOperator.get(cacheKey);
            if (cached != null) {
                return JsonUtils.fromJson(cached.toString(), voClass);
            }
        } catch (Exception ex) {
            log.warn("看板缓存读取异常，回源查询: type={}", type, ex);
        }
        T result = supplier.get();
        try {
            redisOperator.set(cacheKey, JsonUtils.toJson(result), CACHE_TTL);
        } catch (Exception ex) {
            log.warn("看板缓存写入异常，不影响查询: type={}", type, ex);
        }
        return result;
    }

    private String buildCacheKey(String type, DashboardRange range) {
        return RedisConstants.DASHBOARD_CACHE_PREFIX + RedisConstants.KEY_SEPARATOR
                + type + RedisConstants.KEY_SEPARATOR
                + range.rangeType()
                + (range.startTime() != null ? RedisConstants.KEY_SEPARATOR + range.startTime() : "")
                + (range.endTime() != null ? RedisConstants.KEY_SEPARATOR + range.endTime() : "");
    }

    private DashboardRange resolveRange(DashboardRangeQuery query) {
        DashboardRangeQuery safeQuery = query == null ? new DashboardRangeQuery() : query;
        String rangeType = StrUtils.trimToLowerCase(StrUtils.defaultIfBlank(safeQuery.getRangeType(), RANGE_TODAY));
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
            default -> throw new IllegalArgumentException("Unexpected range type: " + rangeType);
        };
    }

    private DashboardRange resolveCustomRange(DashboardRangeQuery query) {
        return new DashboardRange(RANGE_CUSTOM, query.getStartTime(), query.getEndTime());
    }

    private long count(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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
