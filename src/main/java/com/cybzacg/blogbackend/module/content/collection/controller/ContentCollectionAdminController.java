package com.cybzacg.blogbackend.module.content.collection.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.content.collection.model.admin.CollectionPageQuery;
import com.cybzacg.blogbackend.module.content.collection.model.admin.CollectionVO;
import com.cybzacg.blogbackend.module.content.collection.model.user.CollectionFolderVO;
import com.cybzacg.blogbackend.module.content.collection.service.CollectionAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Content收藏后台管理控制器。
 *
 * <p>负责对外暴露Content收藏后台管理相关接口，并作为请求进入业务层的统一入口。
 */
@RestController
@RequestMapping("/api/sys/collections")
@Tag(name = "后台收藏管理")
@RequiredArgsConstructor
public class ContentCollectionAdminController {
    private final CollectionAdminService collectionAdminService;

    @GetMapping("/folders")
    @Operation(summary = "分页查询收藏夹")
    @PreAuthorize("@permission.hasPermission('content:collection:query')")
    public Result<PageResult<CollectionFolderVO>> pageFolders(CollectionPageQuery query) {
        return Result.success(collectionAdminService.pageFolders(query));
    }

    @GetMapping
    @Operation(summary = "分页查询收藏记录")
    @PreAuthorize("@permission.hasPermission('content:collection:query')")
    public Result<PageResult<CollectionVO>> pageCollections(CollectionPageQuery query) {
        return Result.success(collectionAdminService.pageCollections(query));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除收藏记录")
    @PreAuthorize("@permission.hasPermission('content:collection:delete')")
    public Result<Void> deleteCollection(@PathVariable Long id) {
        collectionAdminService.deleteCollection(id);
        return Result.success();
    }
}
