package com.cybzacg.blogbackend.enums.article;

import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.Getter;

/**
 * 文章审核动作枚举。
 */
@Getter
public enum ArticleReviewActionEnum {
    SUBMIT("submit", "提交审核"),
    RESUBMIT("resubmit", "重新提交"),
    APPROVE("approve", "审核通过"),
    REJECT("reject", "审核拒绝"),
    REPAIR("repair", "状态修正");

    private final String code;
    private final String label;

    ArticleReviewActionEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ArticleReviewActionEnum fromCode(String code) {
        if (StrUtils.isBlank(code)) {
            return null;
        }
        for (ArticleReviewActionEnum item : values()) {
            if (item.code.equals(StrUtils.trim(code))) {
                return item;
            }
        }
        return null;
    }

    public static boolean contains(String code) {
        return fromCode(code) != null;
    }

    public static String resolveLabel(String code) {
        ArticleReviewActionEnum action = fromCode(code);
        return action == null ? null : action.getLabel();
    }
}
