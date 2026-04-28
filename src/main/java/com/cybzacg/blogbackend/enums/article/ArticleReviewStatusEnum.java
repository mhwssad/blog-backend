package com.cybzacg.blogbackend.enums.article;

import lombok.Getter;

/**
 * 文章审核状态枚举。
 */
@Getter
public enum ArticleReviewStatusEnum {
    NOT_SUBMITTED(0, "未送审"),
    REVIEWING(1, "审核中"),
    APPROVED(2, "审核通过"),
    REJECTED(3, "审核拒绝");

    private final Integer value;
    private final String label;

    ArticleReviewStatusEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    public static ArticleReviewStatusEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (ArticleReviewStatusEnum item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }

    public static boolean contains(Integer value) {
        return fromValue(value) != null;
    }

    public static String resolveLabel(Integer value) {
        ArticleReviewStatusEnum status = fromValue(value);
        return status == null ? null : status.getLabel();
    }
}
