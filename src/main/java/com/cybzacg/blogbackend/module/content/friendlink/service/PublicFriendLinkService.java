package com.cybzacg.blogbackend.module.content.friendlink.service;

import com.cybzacg.blogbackend.module.content.friendlink.model.publics.PublicFriendLinkVO;

import java.util.List;

/**
 * 友情链接公开查询服务。
 */
public interface PublicFriendLinkService {
    List<PublicFriendLinkVO> listEnabled();
}
