package com.cybzacg.blogbackend.module.auth.experience.model.data;

import lombok.Data;

/**
 * 每日经验来源聚合数据项。
 */
@Data
public class ExperienceDailySummaryItem {

    private String sourceType;
    private Integer totalXp;
}
