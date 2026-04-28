package com.cybzacg.blogbackend.module.auth.experience.level;

/**
 * 等级计算工具。
 *
 * <p>根据经验值查对应等级，阈值按降序匹配。
 */
public final class LevelCalculator {

    private LevelCalculator() {
    }

    /**
     * 根据经验值计算对应等级。
     *
     * @param experiencePoints 当前经验值
     * @return 等级（1-10）
     */
    public static int calculateLevel(int experiencePoints) {
        LevelConfig[] configs = LevelConfig.values();
        for (int i = configs.length - 1; i >= 0; i--) {
            if (experiencePoints >= configs[i].getXpThreshold()) {
                return configs[i].getLevel();
            }
        }
        return 1;
    }
}
