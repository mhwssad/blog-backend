package com.cybzacg.blogbackend.module.auth.experience.service;

import com.cybzacg.blogbackend.module.auth.experience.event.XpAwardEvent;
import com.cybzacg.blogbackend.module.auth.experience.model.user.UserLevelInfoVO;

/**
 * 用户经验服务接口。
 */
public interface UserExperienceService {

    /**
     * 处理经验入账事件。
     */
    void awardExperience(XpAwardEvent event);

    /**
     * 获取用户当前等级。
     */
    int getUserLevel(Long userId);

    /**
     * 查询指定用户当前等级展示信息。
     */
    UserLevelInfoVO getLevelInfo(Long userId);

    /**
     * 检查用户等级是否满足指定门槛。
     */
    boolean checkLevelPermission(Long userId, int requiredLevel);
}
