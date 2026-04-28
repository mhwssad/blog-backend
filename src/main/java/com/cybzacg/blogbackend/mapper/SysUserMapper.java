package com.cybzacg.blogbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.domain.SysUser;
import org.apache.ibatis.annotations.Param;

/**
 * @author liujian
 * @description 针对表【sys_user(用户信息表)】的数据库操作Mapper
 * @createDate 2026-03-18 18:50:44
 * @Entity generator.domain.SysUser
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    SysUser selectByUsername(@Param("username") String username);

    SysUser selectByEmail(@Param("email") String email);

    int updateLoginInfo(@Param("userId") Long userId, @Param("ip") String ip);

    int incrementExperiencePoints(@Param("userId") Long userId, @Param("delta") int delta);

    int updateLevel(@Param("userId") Long userId, @Param("level") int level);
}




