package com.cybzacg.blogbackend.mapper.dashboard;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

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
}
