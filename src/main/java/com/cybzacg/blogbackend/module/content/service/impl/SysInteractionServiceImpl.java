package com.cybzacg.blogbackend.module.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysInteraction;
import com.cybzacg.blogbackend.mapper.SysInteractionMapper;
import com.cybzacg.blogbackend.module.content.service.SysInteractionService;
import org.springframework.stereotype.Service;

@Service
public class SysInteractionServiceImpl extends ServiceImpl<SysInteractionMapper, SysInteraction>
        implements SysInteractionService {
}
