package com.cybzacg.blogbackend.module.content.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.content.model.admin.CollectionPageQuery;
import com.cybzacg.blogbackend.module.content.model.admin.CollectionVO;
import com.cybzacg.blogbackend.module.content.model.user.CollectionFolderVO;

/**
 * 收藏后台管理服务接口。
 *
 * <p>定义收藏后台管理相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface CollectionAdminService {
    PageResult<CollectionFolderVO> pageFolders(CollectionPageQuery query);

    PageResult<CollectionVO> pageCollections(CollectionPageQuery query);

    void deleteCollection(Long id);
}
