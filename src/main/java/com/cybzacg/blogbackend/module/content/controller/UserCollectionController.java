package com.cybzacg.blogbackend.module.content.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.content.model.user.CollectionFolderSaveRequest;
import com.cybzacg.blogbackend.module.content.model.user.CollectionFolderVO;
import com.cybzacg.blogbackend.module.content.model.user.CollectionSaveRequest;
import com.cybzacg.blogbackend.module.content.model.user.CollectionVO;
import com.cybzacg.blogbackend.module.content.service.UserCollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户收藏控制器。
 *
 * <p>负责对外暴露用户收藏相关接口，并作为请求进入业务层的统一入口。
 */
@RestController
@Tag(name = "用户收藏行为")
@RequiredArgsConstructor
public class UserCollectionController {
    private final UserCollectionService userCollectionService;

    @GetMapping("/api/user/collection-folders")
    @Operation(summary = "查询我的收藏夹")
    public Result<PageResult<CollectionFolderVO>> pageFolders() {
        return Result.success(userCollectionService.pageFolders());
    }

    @PostMapping("/api/user/collection-folders")
    @Operation(summary = "新增收藏夹")
    public Result<CollectionFolderVO> createFolder(@Valid @RequestBody CollectionFolderSaveRequest request) {
        return Result.success(userCollectionService.createFolder(request));
    }

    @PutMapping("/api/user/collection-folders/{id}")
    @Operation(summary = "修改收藏夹")
    public Result<CollectionFolderVO> updateFolder(@PathVariable Long id,
                                                   @Valid @RequestBody CollectionFolderSaveRequest request) {
        return Result.success(userCollectionService.updateFolder(id, request));
    }

    @DeleteMapping("/api/user/collection-folders/{id}")
    @Operation(summary = "删除收藏夹")
    public Result<Void> deleteFolder(@PathVariable Long id) {
        userCollectionService.deleteFolder(id);
        return Result.success();
    }

    @GetMapping("/api/user/collections")
    @Operation(summary = "查询我的收藏")
    public Result<PageResult<CollectionVO>> pageCollections() {
        return Result.success(userCollectionService.pageCollections());
    }

    @PostMapping("/api/user/collections")
    @Operation(summary = "新增收藏")
    public Result<Void> createCollection(@Valid @RequestBody CollectionSaveRequest request) {
        userCollectionService.createCollection(request);
        return Result.success();
    }

    @DeleteMapping("/api/user/collections/{id}")
    @Operation(summary = "删除收藏")
    public Result<Void> deleteCollection(@PathVariable Long id) {
        userCollectionService.deleteCollection(id);
        return Result.success();
    }
}
