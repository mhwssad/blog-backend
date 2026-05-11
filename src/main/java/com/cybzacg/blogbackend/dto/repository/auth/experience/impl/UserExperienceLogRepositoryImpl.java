package com.cybzacg.blogbackend.dto.repository.auth.experience.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.dto.domain.auth.UserExperienceLog;
import com.cybzacg.blogbackend.dto.mapper.auth.UserExperienceLogMapper;
import com.cybzacg.blogbackend.dto.repository.auth.experience.UserExperienceLogRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * 经验流水数据访问实现。
 */
@Repository
public class UserExperienceLogRepositoryImpl
        extends ServiceImpl<UserExperienceLogMapper, UserExperienceLog>
        implements UserExperienceLogRepository {

    @Override
    public Integer sumXpByUserAndDate(Long userId, LocalDate date) {
        return baseMapper.sumXpByUserAndDate(userId, date.toString());
    }

    @Override
    public Integer sumXpByUserAndDateAndSource(Long userId, LocalDate date, String sourceType) {
        return baseMapper.sumXpByUserAndDateAndSource(userId, date.toString(), sourceType);
    }

    @Override
    public IPage<UserExperienceLog> pageByConditions(Long userId, String sourceType,
                                                      LocalDate startDate, LocalDate endDate,
                                                      Integer current, Integer size) {
        Page<UserExperienceLog> page = new Page<>(current, size);
        LambdaQueryWrapper<UserExperienceLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, UserExperienceLog::getUserId, userId)
                .eq(sourceType != null, UserExperienceLog::getSourceType, sourceType)
                .ge(startDate != null, UserExperienceLog::getLogDate, startDate)
                .le(endDate != null, UserExperienceLog::getLogDate, endDate)
                .orderByDesc(UserExperienceLog::getCreatedAt);
        return page(page, wrapper);
    }
}
