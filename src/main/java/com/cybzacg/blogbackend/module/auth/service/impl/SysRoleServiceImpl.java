package com.cybzacg.blogbackend.module.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysRole;
import com.cybzacg.blogbackend.mapper.SysRoleMapper;
import com.cybzacg.blogbackend.module.auth.service.SysRoleService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author liujian
* @description 针对表【sys_role(系统角色表)】的数据库操作Service实现
* @createDate 2026-03-18 18:50:44
*/
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole>
    implements SysRoleService{

    @Override
    public List<String> listRoleCodesByUserId(Long userId) {
        List<String> roleCodes = baseMapper.selectRoleCodesByUserId(userId);
        return roleCodes != null ? roleCodes : List.of();
    }
}




