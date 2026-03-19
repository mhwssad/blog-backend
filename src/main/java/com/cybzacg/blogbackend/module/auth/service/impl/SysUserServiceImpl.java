package com.cybzacg.blogbackend.module.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.mapper.SysUserMapper;
import com.cybzacg.blogbackend.module.auth.service.SysUserService;
import org.springframework.stereotype.Service;

/**
* @author liujian
* @description 针对表【sys_user(用户信息表)】的数据库操作Service实现
* @createDate 2026-03-18 18:50:44
*/
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
    implements SysUserService{

    @Override
    public SysUser getByUsername(String username) {
        return baseMapper.selectByUsername(username);
    }

    @Override
    public SysUser getByEmail(String email) {
        return baseMapper.selectByEmail(email);
    }

    @Override
    public boolean updateLoginInfo(Long userId, String ip) {
        return baseMapper.updateLoginInfo(userId, ip) > 0;
    }
}




