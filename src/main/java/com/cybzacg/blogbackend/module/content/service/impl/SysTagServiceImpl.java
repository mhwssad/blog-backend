package com.cybzacg.blogbackend.module.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysTag;
import com.cybzacg.blogbackend.mapper.SysTagMapper;
import com.cybzacg.blogbackend.module.content.service.SysTagService;
import org.springframework.stereotype.Service;

@Service
public class SysTagServiceImpl extends ServiceImpl<SysTagMapper, SysTag>
        implements SysTagService {
}
