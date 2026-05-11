package com.cybzacg.blogbackend.dto.mapper.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.auth.SysRole;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author liujian
 * @description 针对表【sys_role(系统角色表)】的数据库操作Mapper
 * @createDate 2026-03-18 18:50:44
 * @Entity generator.domain.SysRole
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}
