package com.cybzacg.blogbackend.module.content.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.content.model.user.CollectionFolderSaveRequest;
import com.cybzacg.blogbackend.module.content.model.user.CollectionFolderVO;
import com.cybzacg.blogbackend.module.content.model.user.CollectionSaveRequest;
import com.cybzacg.blogbackend.module.content.model.user.CollectionVO;

/**
 * 用户收藏服务接口。
 *
 * <p>定义用户收藏相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface UserCollectionService {
    PageResult<CollectionFolderVO> pageFolders();

    CollectionFolderVO createFolder(CollectionFolderSaveRequest request);

    CollectionFolderVO updateFolder(Long id, CollectionFolderSaveRequest request);

    void deleteFolder(Long id);

    PageResult<CollectionVO> pageCollections();

    void createCollection(CollectionSaveRequest request);

    void deleteCollection(Long id);
}
