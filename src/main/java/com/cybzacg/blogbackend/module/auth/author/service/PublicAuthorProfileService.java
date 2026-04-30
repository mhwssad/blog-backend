package com.cybzacg.blogbackend.module.auth.author.service;

import com.cybzacg.blogbackend.module.auth.author.model.publics.PublicAuthorProfileVO;

/**
 * 公开作者主页服务。
 */
public interface PublicAuthorProfileService {

    /**
     * 查询指定用户的公开作者主页摘要。
     */
    PublicAuthorProfileVO getAuthorProfile(Long userId);
}
