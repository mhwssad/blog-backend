package com.cybzacg.blogbackend.dto.repository.auth.experience;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.auth.UserExperienceLog;

import java.time.LocalDate;

/**
 * 经验流水数据访问接口。
 */
public interface UserExperienceLogRepository extends IService<UserExperienceLog> {

    /**
     * 查询用户某天的经验总量。
     */
    Integer sumXpByUserAndDate(Long userId, LocalDate date);

    /**
     * 查询用户某天某来源的经验总量。
     */
    Integer sumXpByUserAndDateAndSource(Long userId, LocalDate date, String sourceType);

    /**
     * 管理端条件分页查询。
     */
    IPage<UserExperienceLog> pageByConditions(Long userId, String sourceType, LocalDate startDate, LocalDate endDate,
                                               Integer current, Integer size);
}
