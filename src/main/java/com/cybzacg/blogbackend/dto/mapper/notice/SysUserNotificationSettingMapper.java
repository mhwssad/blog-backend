package com.cybzacg.blogbackend.dto.mapper.notice;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cybzacg.blogbackend.dto.domain.notice.SysUserNotificationSetting;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户通知偏好设置 Mapper。
 */
@Mapper
public interface SysUserNotificationSettingMapper
    extends BaseMapper<SysUserNotificationSetting> {}
