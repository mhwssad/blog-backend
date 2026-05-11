package com.cybzacg.blogbackend.dto.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.system.SysLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author liujian
 * @description 针对表【sys_log(系统操作日志表)】的数据库操作Mapper
 * @createDate 2026-03-18 18:50:44
 * @Entity generator.domain.SysLog
 */
@Mapper
public interface SysLogMapper extends BaseMapper<SysLog> {}
