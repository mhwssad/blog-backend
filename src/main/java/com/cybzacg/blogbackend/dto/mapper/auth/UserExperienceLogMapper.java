package com.cybzacg.blogbackend.dto.mapper.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.auth.UserExperienceLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户经验流水 Mapper。
 */
@Mapper
public interface UserExperienceLogMapper extends BaseMapper<UserExperienceLog> {
    Integer sumXpByUserAndDate(Long userId, String logDate);

    Integer sumXpByUserAndDateAndSource(
        Long userId,
        String logDate,
        String sourceType
    );
}
