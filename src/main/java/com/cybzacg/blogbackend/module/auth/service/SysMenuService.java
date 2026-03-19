package com.cybzacg.blogbackend.module.auth.service;

import com.cybzacg.blogbackend.domain.SysMenu;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 系统菜单服务接口。
 *
 * <p>定义系统菜单相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface SysMenuService extends IService<SysMenu> {

    List<String> listPermissionsByUserId(Long userId);

    List<SysMenu> listMenusByUserId(Long userId);
}
