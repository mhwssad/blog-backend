package com.cybzacg.blogbackend.module.auth.experience.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.auth.experience.model.admin.*;

import java.util.List;

/**
 * 经验管理端服务接口。
 */
public interface ExperienceAdminService {

    /**
     * 查看用户经验来源汇总。
     */
    UserExperienceSummaryVO getUserExperienceSummary(Long userId);

    /**
     * 经验流水分页查询。
     */
    PageResult<ExperienceLogVO> pageExperienceLogs(ExperienceLogPageQuery query);

    /**
     * 手动调整等级或经验。
     */
    void adjustUserLevelOrExperience(Long userId, UserLevelAdjustRequest request);

    /**
     * 查看经验来源配置列表。
     */
    List<ExperienceSourceConfigVO> listSourceConfigs();

    /**
     * 更新经验来源配置。
     */
    void updateSourceConfig(String configKey, String configValue);
}
