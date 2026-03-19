package com.cybzacg.blogbackend.module.content.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.content.model.admin.TagSaveRequest;
import com.cybzacg.blogbackend.module.content.model.admin.TagVO;
import com.cybzacg.blogbackend.module.content.service.TagAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Content标签后台管理控制器。
 *
 * <p>负责对外暴露Content标签后台管理相关接口，并作为请求进入业务层的统一入口。
 */
@RestController
@RequestMapping("/api/sys/tags")
@Tag(name = "后台标签管理")
@RequiredArgsConstructor
public class ContentTagAdminController {
    private final TagAdminService tagAdminService;

    @GetMapping
    @Operation(summary = "查询标签列表")
    @PreAuthorize("@permission.hasPermission('content:tag:query')")
    public Result<List<TagVO>> listTags() {
        return Result.success(tagAdminService.listTags());
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询标签详情")
    @PreAuthorize("@permission.hasPermission('content:tag:query')")
    public Result<TagVO> getTag(@PathVariable Long id) {
        return Result.success(tagAdminService.getTag(id));
    }

    @PostMapping
    @Operation(summary = "新增标签")
    @PreAuthorize("@permission.hasPermission('content:tag:create')")
    public Result<TagVO> createTag(@Valid @RequestBody TagSaveRequest request) {
        return Result.success(tagAdminService.createTag(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改标签")
    @PreAuthorize("@permission.hasPermission('content:tag:update')")
    public Result<TagVO> updateTag(@PathVariable Long id,
                                   @Valid @RequestBody TagSaveRequest request) {
        return Result.success(tagAdminService.updateTag(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除标签")
    @PreAuthorize("@permission.hasPermission('content:tag:delete')")
    public Result<Void> deleteTag(@PathVariable Long id) {
        tagAdminService.deleteTag(id);
        return Result.success();
    }
}
