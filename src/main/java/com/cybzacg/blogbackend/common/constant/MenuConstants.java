package com.cybzacg.blogbackend.common.constant;

/**
 * 菜单相关常量。<p>定义菜单层级类型（目录 / 菜单 / 按钮）和根节点标识。
 */
public final class MenuConstants {
    public static final Long ROOT_PARENT_ID = 0L;

    public static final String TYPE_CATALOG = "C";
    public static final String TYPE_MENU = "M";
    public static final String TYPE_BUTTON = "B";

    private MenuConstants() {
    }
}
