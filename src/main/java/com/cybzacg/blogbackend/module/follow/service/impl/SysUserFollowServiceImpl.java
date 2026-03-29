package com.cybzacg.blogbackend.module.follow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysUserFollow;
import com.cybzacg.blogbackend.mapper.SysUserFollowMapper;
import com.cybzacg.blogbackend.module.follow.service.SysUserFollowService;
import org.springframework.stereotype.Service;

/**
 * 用户关注关系基础服务实现。
 */
@Service
public class SysUserFollowServiceImpl extends ServiceImpl<SysUserFollowMapper, SysUserFollow>
        implements SysUserFollowService {
}
