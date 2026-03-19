package com.cybzacg.blogbackend.module.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysUserFootprint;
import com.cybzacg.blogbackend.mapper.SysUserFootprintMapper;
import com.cybzacg.blogbackend.module.content.service.SysUserFootprintService;
import org.springframework.stereotype.Service;

@Service
public class SysUserFootprintServiceImpl extends ServiceImpl<SysUserFootprintMapper, SysUserFootprint>
        implements SysUserFootprintService {
}
