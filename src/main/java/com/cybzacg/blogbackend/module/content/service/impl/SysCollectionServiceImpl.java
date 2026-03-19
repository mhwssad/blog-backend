package com.cybzacg.blogbackend.module.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysCollection;
import com.cybzacg.blogbackend.mapper.SysCollectionMapper;
import com.cybzacg.blogbackend.module.content.service.SysCollectionService;
import org.springframework.stereotype.Service;

@Service
public class SysCollectionServiceImpl extends ServiceImpl<SysCollectionMapper, SysCollection>
        implements SysCollectionService {
}
