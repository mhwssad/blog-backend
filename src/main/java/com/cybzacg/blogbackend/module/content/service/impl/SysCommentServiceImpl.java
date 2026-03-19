package com.cybzacg.blogbackend.module.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.SysComment;
import com.cybzacg.blogbackend.mapper.SysCommentMapper;
import com.cybzacg.blogbackend.module.content.service.SysCommentService;
import org.springframework.stereotype.Service;

@Service
public class SysCommentServiceImpl extends ServiceImpl<SysCommentMapper, SysComment>
        implements SysCommentService {
}
