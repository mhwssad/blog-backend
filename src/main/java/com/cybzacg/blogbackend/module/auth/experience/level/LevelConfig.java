package com.cybzacg.blogbackend.module.auth.experience.level;

import lombok.Getter;

/**
 * 用户等级配置枚举。
 *
 * <p>定义 1-10 级的经验阈值、称号、权限门槛和 AI 每日额度。
 */
@Getter
public enum LevelConfig {

    LEVEL_1(1, 0, "新手", 1, 5),
    LEVEL_2(2, 100, "入门", 1, 10),
    LEVEL_3(3, 500, "进阶", 2, 15),
    LEVEL_4(4, 1500, "熟练", 2, 20),
    LEVEL_5(5, 4000, "精通", 3, 30),
    LEVEL_6(6, 8000, "专家", 3, 40),
    LEVEL_7(7, 15000, "大师", 4, 50),
    LEVEL_8(8, 30000, "宗师", 5, 60),
    LEVEL_9(9, 60000, "传奇", 6, 80),
    LEVEL_10(10, 100000, "至尊", 10, 100);

    private final int level;
    private final int xpThreshold;
    private final String title;
    private final int permissionGate;
    private final int aiDailyQuota;

    LevelConfig(int level, int xpThreshold, String title, int permissionGate, int aiDailyQuota) {
        this.level = level;
        this.xpThreshold = xpThreshold;
        this.title = title;
        this.permissionGate = permissionGate;
        this.aiDailyQuota = aiDailyQuota;
    }

    public static LevelConfig getByLevel(int level) {
        for (LevelConfig config : values()) {
            if (config.level == level) {
                return config;
            }
        }
        return null;
    }
}
