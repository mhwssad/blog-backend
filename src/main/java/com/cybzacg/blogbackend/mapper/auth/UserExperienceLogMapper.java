package com.cybzacg.blogbackend.mapper.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.domain.auth.UserExperienceLog;

/**
 * 用户经验流水 Mapper。
 */
public interface UserExperienceLogMapper extends BaseMapper<UserExperienceLog> {

    Integer sumXpByUserAndDate(Long userId, String logDate);

    Integer sumXpByUserAndDateAndSource(Long userId, String logDate, String sourceType);
}
