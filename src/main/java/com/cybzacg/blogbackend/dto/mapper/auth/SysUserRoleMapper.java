package com.cybzacg.blogbackend.dto.mapper.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.auth.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author liujian
 * @description 针对表【sys_user_role(用户角色关联表)】的数据库操作Mapper
 * @createDate 2026-03-18 18:50:44
 * @Entity generator.domain.SysUserRole
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {}
