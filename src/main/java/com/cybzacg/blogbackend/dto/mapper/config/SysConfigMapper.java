package com.cybzacg.blogbackend.dto.mapper.config;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.config.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author liujian
 * @description 针对表【sys_config(系统配置表)】的数据库操作Mapper
 * @createDate 2026-03-18 18:50:44
 * @Entity generator.domain.SysConfig
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {
    SysConfig selectByConfigKey(@Param("configKey") String configKey);
}
