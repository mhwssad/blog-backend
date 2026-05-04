package com.cybzacg.blogbackend.module.dashboard;

import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.mapper.dashboard.DashboardMetricsMapper;
import com.cybzacg.blogbackend.module.dashboard.model.admin.*;
import com.cybzacg.blogbackend.module.dashboard.service.impl.DashboardAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * DashboardAdminServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class DashboardAdminServiceImplTest {
    @Mock
    private DashboardMetricsMapper dashboardMetricsMapper;

    private DashboardAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DashboardAdminServiceImpl(dashboardMetricsMapper);
    }

    @Test
    void getCommunityShouldReturnForumMetricsAndHotSections() {
        DashboardHotSectionVO section = new DashboardHotSectionVO();
        section.setSectionId(10L);
        section.setSectionName("综合讨论");
        section.setPostCount(8L);
        section.setReplyCount(12L);
        section.setHotValue(20L);

        when(dashboardMetricsMapper.countChatMessages(any(), any())).thenReturn(30L);
        when(dashboardMetricsMapper.countLobbyMessages(any(), any())).thenReturn(9L);
        when(dashboardMetricsMapper.countGroups(any(), any())).thenReturn(3L);
        when(dashboardMetricsMapper.countForumPosts(any(), any())).thenReturn(8L);
        when(dashboardMetricsMapper.countForumReplies(any(), any())).thenReturn(12L);
        when(dashboardMetricsMapper.listHotSections(any(), any(), eq(5))).thenReturn(List.of(section));

        DashboardCommunityVO result = service.getCommunity(customQuery());

        assertEquals(30L, result.getChatMessageCount());
        assertEquals(9L, result.getLobbyMessageCount());
        assertEquals(3L, result.getGroupCount());
        assertEquals(8L, result.getForumPostCount());
        assertEquals(12L, result.getForumReplyCount());
        assertEquals(1, result.getHotSections().size());
        assertEquals("综合讨论", result.getHotSections().get(0).getSectionName());
        assertEquals(20L, result.getHotSections().get(0).getHotValue());
    }

    @Test
    void getAiShouldReturnRagAndAgentMetrics() {
        when(dashboardMetricsMapper.countAiCalls(any(), any(), isNull())).thenReturn(100L);
        when(dashboardMetricsMapper.countAiCalls(any(), any(), eq(1))).thenReturn(90L);
        when(dashboardMetricsMapper.countAiCalls(any(), any(), eq(0))).thenReturn(10L);
        when(dashboardMetricsMapper.countRagCalls(any(), any())).thenReturn(7L);
        when(dashboardMetricsMapper.countAgentTasks(any(), any(), isNull())).thenReturn(11L);
        when(dashboardMetricsMapper.countAgentTasks(any(), any(), eq(2))).thenReturn(8L);
        when(dashboardMetricsMapper.countAgentTasks(any(), any(), eq(3))).thenReturn(2L);

        DashboardAiVO result = service.getAi(customQuery());

        assertEquals(100L, result.getAiCallCount());
        assertEquals(90L, result.getAiSuccessCallCount());
        assertEquals(10L, result.getAiFailedCallCount());
        assertEquals(7L, result.getRagCallCount());
        assertEquals(11L, result.getAgentTaskCount());
        assertEquals(8L, result.getAgentSuccessTaskCount());
        assertEquals(2L, result.getAgentFailedTaskCount());
    }

    @Test
    void getGovernanceShouldReturnHandleDurationAndPunishmentDistribution() {
        DashboardPunishmentDistributionVO mute = new DashboardPunishmentDistributionVO();
        mute.setPunishmentType("mute");
        mute.setCount(4L);
        DashboardPunishmentDistributionVO none = new DashboardPunishmentDistributionVO();
        none.setPunishmentType("none");
        none.setCount(2L);

        when(dashboardMetricsMapper.countReports(any(), any(), isNull())).thenReturn(12L);
        when(dashboardMetricsMapper.countReports(isNull(), isNull(), eq(0))).thenReturn(1L);
        when(dashboardMetricsMapper.countReports(isNull(), isNull(), eq(1))).thenReturn(2L);
        when(dashboardMetricsMapper.countReports(any(), any(), eq(2))).thenReturn(8L);
        when(dashboardMetricsMapper.countReports(any(), any(), eq(3))).thenReturn(3L);
        when(dashboardMetricsMapper.averageReportHandleDurationMinutes(any(), any()))
                .thenReturn(new BigDecimal("15.50"));
        when(dashboardMetricsMapper.listPunishmentDistributions(any(), any())).thenReturn(List.of(mute, none));

        DashboardGovernanceVO result = service.getGovernance(customQuery());

        assertEquals(12L, result.getReportCount());
        assertEquals(1L, result.getPendingReportCount());
        assertEquals(2L, result.getProcessingReportCount());
        assertEquals(8L, result.getHandledReportCount());
        assertEquals(3L, result.getRejectedReportCount());
        assertEquals(new BigDecimal("15.50"), result.getAverageHandleDurationMinutes());
        assertEquals(2, result.getPunishmentDistributions().size());
        assertEquals("mute", result.getPunishmentDistributions().get(0).getPunishmentType());
    }

    @Test
    void getOverviewShouldRejectIllegalRangeType() {
        DashboardRangeQuery query = new DashboardRangeQuery();
        query.setRangeType("yesterday");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getOverview(query));

        assertEquals("时间范围类型仅支持 today/week/month/all/custom", ex.getMessage());
    }

    @Test
    void exportDashboardShouldReturnExcelBytes() {
        when(dashboardMetricsMapper.countRegisteredUsers(any(), any())).thenReturn(1L);
        when(dashboardMetricsMapper.countActiveUsers(any(), any())).thenReturn(2L);
        when(dashboardMetricsMapper.countAuthors()).thenReturn(3L);
        when(dashboardMetricsMapper.countArticles(any(), any())).thenReturn(4L);
        when(dashboardMetricsMapper.countPendingArticleReviews()).thenReturn(5L);
        when(dashboardMetricsMapper.countComments(any(), any())).thenReturn(6L);
        when(dashboardMetricsMapper.countChatMessages(any(), any())).thenReturn(7L);
        when(dashboardMetricsMapper.countAiCalls(any(), any(), isNull())).thenReturn(8L);
        when(dashboardMetricsMapper.countAiCalls(any(), any(), eq(1))).thenReturn(7L);
        when(dashboardMetricsMapper.countAiCalls(any(), any(), eq(0))).thenReturn(1L);
        when(dashboardMetricsMapper.countReports(any(), any(), isNull())).thenReturn(9L);
        when(dashboardMetricsMapper.countReports(isNull(), isNull(), eq(0))).thenReturn(1L);
        when(dashboardMetricsMapper.countReports(isNull(), isNull(), eq(1))).thenReturn(2L);
        when(dashboardMetricsMapper.countReports(any(), any(), eq(2))).thenReturn(6L);
        when(dashboardMetricsMapper.countReports(any(), any(), eq(3))).thenReturn(3L);

        byte[] bytes = service.exportDashboard(customQuery());

        assertTrue(bytes.length > 0);
        assertEquals('P', bytes[0]);
        assertEquals('K', bytes[1]);
    }

    private DashboardRangeQuery customQuery() {
        DashboardRangeQuery query = new DashboardRangeQuery();
        query.setRangeType("custom");
        query.setStartTime(LocalDateTime.of(2026, 5, 1, 0, 0));
        query.setEndTime(LocalDateTime.of(2026, 5, 2, 0, 0));
        return query;
    }
}
