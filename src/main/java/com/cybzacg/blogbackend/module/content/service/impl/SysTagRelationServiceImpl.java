package com.cybzacg.blogbackend.module.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysTagRelation;
import com.cybzacg.blogbackend.mapper.SysTagRelationMapper;
import com.cybzacg.blogbackend.module.content.service.SysTagRelationService;
import org.springframework.stereotype.Service;

@Service
public class SysTagRelationServiceImpl extends ServiceImpl<SysTagRelationMapper, SysTagRelation>
        implements SysTagRelationService {
}
