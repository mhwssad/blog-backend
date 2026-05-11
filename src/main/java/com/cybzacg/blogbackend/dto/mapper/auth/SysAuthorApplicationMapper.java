package com.cybzacg.blogbackend.dto.mapper.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.auth.SysAuthorApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 作者申请 Mapper。
 */
@Mapper
public interface SysAuthorApplicationMapper
    extends BaseMapper<SysAuthorApplication> {}
