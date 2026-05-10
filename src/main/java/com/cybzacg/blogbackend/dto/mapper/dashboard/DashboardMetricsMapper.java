package com.cybzacg.blogbackend.dto.mapper.dashboard;

import com.cybzacg.blogbackend.module.dashboard.model.admin.DashboardHotSectionVO;
import com.cybzacg.blogbackend.module.dashboard.model.admin.DashboardPunishmentDistributionVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台数据看板指标 Mapper。
 */
public interface DashboardMetricsMapper {
    Long countRegisteredUsers(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    Long countActiveUsers(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    Long countAuthors();

    Long countArticles(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    Long countPendingArticleReviews();

    Long countComments(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    Long countLikes(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    Long countCollections(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    Long countChatMessages(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    Long countLobbyMessages(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    Long countGroups(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    Long countAiCalls(@Param("startTime") LocalDateTime startTime,
                      @Param("endTime") LocalDateTime endTime,
                      @Param("successStatus") Integer successStatus);

    Long countReports(@Param("startTime") LocalDateTime startTime,
                      @Param("endTime") LocalDateTime endTime,
                      @Param("status") Integer status);

    Long countForumPosts(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    Long countForumReplies(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    List<DashboardHotSectionVO> listHotSections(@Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime,
                                                @Param("limit") int limit);

    Long countRagCalls(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    Long countAgentTasks(@Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime,
                         @Param("status") Integer status);

    BigDecimal averageReportHandleDurationMinutes(@Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);

    List<DashboardPunishmentDistributionVO> listPunishmentDistributions(@Param("startTime") LocalDateTime startTime,
                                                                        @Param("endTime") LocalDateTime endTime);
}
