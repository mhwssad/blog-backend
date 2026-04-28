package com.cybzacg.blogbackend.module.dashboard.service;

import com.cybzacg.blogbackend.module.dashboard.model.admin.*;

/**
 * 后台数据看板服务。
 */
public interface DashboardAdminService {
    DashboardOverviewVO getOverview(DashboardRangeQuery query);

    DashboardContentVO getContent(DashboardRangeQuery query);

    DashboardCommunityVO getCommunity(DashboardRangeQuery query);

    DashboardAiVO getAi(DashboardRangeQuery query);

    DashboardGovernanceVO getGovernance(DashboardRangeQuery query);
}
