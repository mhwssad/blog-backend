package com.cybzacg.blogbackend.dto.mapper.notice;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.notice.SysNotice;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author liujian
 * @description 针对表【sys_notice(系统通知公告表)】的数据库操作Mapper
 * @createDate 2026-03-18 18:50:44
 * @Entity generator.domain.SysNotice
 */
@Mapper
public interface SysNoticeMapper extends BaseMapper<SysNotice> {}
