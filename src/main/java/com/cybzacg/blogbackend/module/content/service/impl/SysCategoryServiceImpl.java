package com.cybzacg.blogbackend.module.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysCategory;
import com.cybzacg.blogbackend.mapper.SysCategoryMapper;
import com.cybzacg.blogbackend.module.content.service.SysCategoryService;
import org.springframework.stereotype.Service;

@Service
public class SysCategoryServiceImpl extends ServiceImpl<SysCategoryMapper, SysCategory>
        implements SysCategoryService {
}
