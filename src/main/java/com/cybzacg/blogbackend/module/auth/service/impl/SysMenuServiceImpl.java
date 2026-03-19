package com.cybzacg.blogbackend.module.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysMenu;
import com.cybzacg.blogbackend.mapper.SysMenuMapper;
import com.cybzacg.blogbackend.module.auth.service.SysMenuService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author liujian
* @description 针对表【sys_menu(系统菜单表)】的数据库操作Service实现
* @createDate 2026-03-18 18:50:44
*/
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu>
    implements SysMenuService{

    @Override
    public List<String> listPermissionsByUserId(Long userId) {
        List<String> permissions = baseMapper.selectPermissionsByUserId(userId);
        return permissions != null ? permissions : List.of();
    }

    @Override
    public List<SysMenu> listMenusByUserId(Long userId) {
        List<SysMenu> menus = baseMapper.selectMenusByUserId(userId);
        return menus != null ? menus : List.of();
    }
}




