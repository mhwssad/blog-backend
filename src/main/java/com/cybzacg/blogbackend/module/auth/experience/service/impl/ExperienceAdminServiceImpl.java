package com.cybzacg.blogbackend.module.auth.experience.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.config.SysConfig;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.domain.auth.UserExperienceLog;
import com.cybzacg.blogbackend.enums.experience.ExperienceSourceTypeEnum;
import com.cybzacg.blogbackend.module.auth.experience.convert.ExperienceModelMapper;
import com.cybzacg.blogbackend.module.auth.experience.level.LevelCalculator;
import com.cybzacg.blogbackend.module.auth.experience.level.LevelConfig;
import com.cybzacg.blogbackend.module.auth.experience.model.admin.*;
import com.cybzacg.blogbackend.module.auth.experience.repository.UserExperienceLogRepository;
import com.cybzacg.blogbackend.module.auth.experience.service.ExperienceAdminService;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.config.service.SysConfigService;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 经验管理端服务实现。
 */
@Service
@RequiredArgsConstructor
public class ExperienceAdminServiceImpl implements ExperienceAdminService {

    private final SysUserRepository sysUserRepository;
    private final UserExperienceLogRepository experienceLogRepository;
    private final SysConfigService sysConfigService;
    private final ExperienceModelMapper experienceModelMapper;

    @Override
    public UserExperienceSummaryVO getUserExperienceSummary(Long userId) {
        SysUser user = sysUserRepository.getById(userId);
        ExceptionThrowerCore.throwBusinessIfNull(user, ResultErrorCode.USER_NOT_FOUND);

        int level = user.getUserLevel() != null ? user.getUserLevel() : 1;
        LevelConfig levelConfig = LevelConfig.getByLevel(level);
        String title = levelConfig != null ? levelConfig.getTitle() : "";

        LocalDate today = LocalDate.now();
        Integer todayXp = experienceLogRepository.sumXpByUserAndDate(userId, today);

        UserExperienceSummaryVO.UserExperienceSummaryVOBuilder builder = UserExperienceSummaryVO.builder()
                .userId(userId)
                .level(level)
                .title(title)
                .experiencePoints(user.getExperiencePoints() != null ? user.getExperiencePoints() : 0)
                .todayXp(todayXp != null ? todayXp : 0);

        for (ExperienceSourceTypeEnum source : ExperienceSourceTypeEnum.values()) {
            Integer sourceXp = experienceLogRepository.sumXpByUserAndDateAndSource(userId, today, source.getValue());
            int xp = sourceXp != null ? sourceXp : 0;
            switch (source) {
                case DAILY_LOGIN -> builder.dailyLoginXp(xp);
                case ARTICLE_PUBLISH -> builder.articlePublishXp(xp);
                case COMMENT_CREATE -> builder.commentCreateXp(xp);
                case LIKE_GIVEN -> builder.likeGivenXp(xp);
                case LIKE_RECEIVED -> builder.likeReceivedXp(xp);
                case CHAT_MESSAGE -> builder.chatMessageXp(xp);
            }
        }

        return builder.build();
    }

    @Override
    public PageResult<ExperienceLogVO> pageExperienceLogs(ExperienceLogPageQuery query) {
        IPage<UserExperienceLog> page = experienceLogRepository.pageByConditions(
                query.getUserId(), query.getSourceType(),
                query.getStartDate(), query.getEndDate(),
                query.getCurrent(), query.getSize());

        List<ExperienceLogVO> records = page.getRecords().stream()
                .map(experienceModelMapper::toLogVO)
                .toList();

        return PageResult.<ExperienceLogVO>builder()
                .total(page.getTotal())
                .current(page.getCurrent())
                .size(page.getSize())
                .records(records)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustUserLevelOrExperience(Long userId, UserLevelAdjustRequest request) {
        SysUser user = sysUserRepository.getById(userId);
        ExceptionThrowerCore.throwBusinessIfNull(user, ResultErrorCode.USER_NOT_FOUND);

        if ("level".equals(request.getAdjustType())) {
            ExceptionThrowerCore.throwBusinessIf(request.getValue() < 1 || request.getValue() > 10,
                    ResultErrorCode.ILLEGAL_ARGUMENT, "等级必须在 1-10 之间");
            sysUserRepository.updateLevel(userId, request.getValue());
        } else if ("experience".equals(request.getAdjustType())) {
            sysUserRepository.incrementExperiencePoints(userId, request.getValue());
            SysUser updated = sysUserRepository.getById(userId);
            if (updated != null) {
                int newLevel = LevelCalculator.calculateLevel(updated.getExperiencePoints());
                if (newLevel != updated.getUserLevel()) {
                    sysUserRepository.updateLevel(userId, newLevel);
                }
            }
        } else {
            ExceptionThrowerCore.throwBusinessEx("不支持的调整类型: " + request.getAdjustType());
        }
    }

    @Override
    public List<ExperienceSourceConfigVO> listSourceConfigs() {
        List<ExperienceSourceConfigVO> configs = new ArrayList<>();
        for (ExperienceSourceTypeEnum source : ExperienceSourceTypeEnum.values()) {
            SysConfig valueConfig = sysConfigService.getByConfigKey(source.getConfigValueKey());
            configs.add(ExperienceSourceConfigVO.builder()
                    .configKey(source.getConfigValueKey())
                    .configName(source.getLabel() + "经验值")
                    .configValue(valueConfig != null ? valueConfig.getConfigValue() : "0")
                    .remark("每次" + source.getLabel() + "获得的经验值")
                    .build());

            SysConfig enabledConfig = sysConfigService.getByConfigKey(source.getConfigEnabledKey());
            configs.add(ExperienceSourceConfigVO.builder()
                    .configKey(source.getConfigEnabledKey())
                    .configName(source.getLabel() + "开关")
                    .configValue(enabledConfig != null ? enabledConfig.getConfigValue() : "1")
                    .remark("1-启用 0-停用")
                    .build());
        }
        return configs;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSourceConfig(String configKey, String configValue) {
        SysConfig config = sysConfigService.getByConfigKey(configKey);
        ExceptionThrowerCore.throwBusinessIfNull(config, ResultErrorCode.ILLEGAL_ARGUMENT, "配置项不存在: " + configKey);
        config.setConfigValue(configValue);
        sysConfigService.updateConfig(config);
    }
}
