package com.cybzacg.blogbackend.module.content.friendlink.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.content.friendlink.model.admin.FriendLinkPageQuery;
import com.cybzacg.blogbackend.module.content.friendlink.model.admin.FriendLinkSaveRequest;
import com.cybzacg.blogbackend.module.content.friendlink.model.admin.FriendLinkVO;

/**
 * 友情链接后台管理服务。
 */
public interface FriendLinkAdminService {
    PageResult<FriendLinkVO> page(FriendLinkPageQuery query);

    FriendLinkVO getById(Long id);

    FriendLinkVO create(FriendLinkSaveRequest request);

    FriendLinkVO update(Long id, FriendLinkSaveRequest request);

    void updateStatus(Long id, Integer status);

    void delete(Long id);
}
