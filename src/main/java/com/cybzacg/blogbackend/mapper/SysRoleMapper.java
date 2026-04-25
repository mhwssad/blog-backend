package com.cybzacg.blogbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.domain.SysRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author liujian
 * @description 针对表【sys_role(系统角色表)】的数据库操作Mapper
 * @createDate 2026-03-18 18:50:44
 * @Entity generator.domain.SysRole
 */
public interface SysRoleMapper extends BaseMapper<SysRole> {

    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}




