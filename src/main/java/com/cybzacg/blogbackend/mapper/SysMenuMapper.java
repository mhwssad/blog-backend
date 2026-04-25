package com.cybzacg.blogbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.domain.SysMenu;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author liujian
 * @description 针对表【sys_menu(系统菜单表)】的数据库操作Mapper
 * @createDate 2026-03-18 18:50:44
 * @Entity generator.domain.SysMenu
 */
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    List<String> selectPermissionsByUserId(@Param("userId") Long userId);

    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);
}




