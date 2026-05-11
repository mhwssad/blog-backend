package com.cybzacg.blogbackend.dto.repository.content;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.content.BlogFriendLink;

import java.util.List;

/**
 * 友情链接 Repository。
 */
public interface BlogFriendLinkRepository extends IService<BlogFriendLink> {
    /**
     * 查询启用友情链接，按排序值升序、ID 降序。
     */
    List<BlogFriendLink> listEnabled();
}
