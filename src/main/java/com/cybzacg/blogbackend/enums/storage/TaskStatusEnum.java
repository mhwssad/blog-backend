package com.cybzacg.blogbackend.enums.storage;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * 文件上传任务状态枚举
 *
 * @author system
 * @since 2025/12/28
 */
@Getter
public enum TaskStatusEnum {

    /**
     * 初始化
     */
    INIT(0, "初始化"),

    /**
     * 上传中
     */
    UPLOADING(1, "上传中"),

    /**
     * 合并中
     */
    MERGING(2, "合并中"),

    /**
     * 已完成
     */
    COMPLETED(3, "已完成"),

    /**
     * 失败
     */
    FAILED(4, "失败"),

    /**
     * 已取消
     */
    CANCELLED(5, "已取消");

    private final Integer value;
    private final String label;

    TaskStatusEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 代码
     * @return 枚举
     */
    public static TaskStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (TaskStatusEnum taskStatusEnum : values()) {
            if (taskStatusEnum.value.equals(code)) {
                return taskStatusEnum;
            }
        }
        return null;
    }

    /**
     * 获取未完成的状态列表
     *
     * @return 未完成状态列表
     */
    public static List<TaskStatusEnum> getIncompleteStatuses() {
        return Arrays.asList(INIT, UPLOADING, MERGING, FAILED, CANCELLED);
    }

    /**
     * 判断状态是否可以转换到目标状态
     *
     * @param targetStatus 目标状态
     * @return 是否可以转换
     */
    public boolean canTransitionTo(TaskStatusEnum targetStatus) {
        // 允许的状态转换规则
        if (this == INIT && targetStatus == UPLOADING) {
            return true;
        }
        if (this == UPLOADING && (targetStatus == MERGING || targetStatus == COMPLETED || targetStatus == FAILED || targetStatus == CANCELLED)) {
            return true;
        }
        if (this == MERGING && (targetStatus == COMPLETED || targetStatus == FAILED)) {
            return true;
        }
        if (this == FAILED && targetStatus == UPLOADING) {
            return true;
        }
        if (this == INIT && targetStatus == CANCELLED) {
            return true;
        }
        if (this == UPLOADING && targetStatus == CANCELLED) {
            return true;
        }
        if (this == MERGING && targetStatus == CANCELLED) {
            return true;
        }
        return false;
    }
}

