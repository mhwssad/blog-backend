package com.cybzacg.blogbackend.dto.mapper.notice;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.notice.SysUserNotice;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author liujian
 * @description 针对表【sys_user_notice(用户通知公告关联表)】的数据库操作Mapper
 * @createDate 2026-03-18 18:50:44
 * @Entity generator.domain.SysUserNotice
 */
@Mapper
public interface SysUserNoticeMapper extends BaseMapper<SysUserNotice> {}
