package com.cybzacg.blogbackend.module.auth.notice.convert;

import com.cybzacg.blogbackend.common.constant.NoticeConstants;
import com.cybzacg.blogbackend.domain.notice.SysNotice;
import com.cybzacg.blogbackend.module.auth.notice.model.admin.SysNoticeAdminVO;
import com.cybzacg.blogbackend.module.auth.notice.model.admin.UserNoticeVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 系统通知对象转换器，处理通知视图映射及目标用户 ID 解析。
 */
@Mapper(componentModel = "spring")
public interface SysNoticeModelMapper {
    @Mapping(target = "targetUserIds", ignore = true)
    SysNoticeAdminVO toNoticeAdminVO(SysNotice notice);

    @Mapping(target = "isRead", ignore = true)
    @Mapping(target = "readTime", ignore = true)
    UserNoticeVO toUserNoticeVO(SysNotice notice);

    default SysNoticeAdminVO toNoticeAdminVO(SysNotice notice, List<Long> targetUserIds) {
        SysNoticeAdminVO vo = toNoticeAdminVO(notice);
        vo.setTargetUserIds(targetUserIds);
        return vo;
    }

    default UserNoticeVO toUserNoticeVO(SysNotice notice, boolean isRead, LocalDateTime readTime) {
        UserNoticeVO vo = toUserNoticeVO(notice);
        vo.setIsRead(isRead ? NoticeConstants.READ_READ : NoticeConstants.READ_UNREAD);
        vo.setReadTime(readTime);
        return vo;
    }

    default List<Long> toIdList(String targetUserIds) {
        if (targetUserIds == null || targetUserIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(targetUserIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Long::valueOf)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
