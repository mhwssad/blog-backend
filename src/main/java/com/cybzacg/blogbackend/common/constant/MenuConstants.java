package com.cybzacg.blogbackend.common.constant;

/**
 * 菜单相关常量。<p>定义菜单层级类型（目录 / 菜单 / 按钮）和根节点标识。
 */
public final class MenuConstants {
    public static final Long ROOT_PARENT_ID = 0L;
    /** 超级管理员通配符权限菜单 ID，仅在初始化脚本中创建，后台接口不可见/不可分配。 */
    public static final Long WILDCARD_MENU_ID = 1L;

    public static final String TYPE_CATALOG = "C";
    public static final String TYPE_MENU = "M";
    public static final String TYPE_BUTTON = "B";

    private MenuConstants() {
    }
}
