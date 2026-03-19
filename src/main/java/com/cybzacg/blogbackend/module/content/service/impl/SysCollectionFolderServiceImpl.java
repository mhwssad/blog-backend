package com.cybzacg.blogbackend.module.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysCollectionFolder;
import com.cybzacg.blogbackend.mapper.SysCollectionFolderMapper;
import com.cybzacg.blogbackend.module.content.service.SysCollectionFolderService;
import org.springframework.stereotype.Service;

@Service
public class SysCollectionFolderServiceImpl extends ServiceImpl<SysCollectionFolderMapper, SysCollectionFolder>
        implements SysCollectionFolderService {
}
