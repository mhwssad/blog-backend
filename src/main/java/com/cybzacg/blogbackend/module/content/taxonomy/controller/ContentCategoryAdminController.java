package com.cybzacg.blogbackend.module.content.taxonomy.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.model.admin.StatusUpdateRequest;
import com.cybzacg.blogbackend.module.content.taxonomy.model.admin.CategoryAdminVO;
import com.cybzacg.blogbackend.module.content.taxonomy.model.admin.CategorySaveRequest;
import com.cybzacg.blogbackend.module.content.taxonomy.model.admin.CategoryTreeVO;
import com.cybzacg.blogbackend.module.content.taxonomy.service.CategoryAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Content分类后台管理控制器。
 *
 * <p>负责对外暴露Content分类后台管理相关接口，并作为请求进入业务层的统一入口。
 */
@RestController
@RequestMapping("/api/sys/categories")
@Tag(name = "后台分类管理")
@RequiredArgsConstructor
public class ContentCategoryAdminController {
    private final CategoryAdminService categoryAdminService;

    @GetMapping("/tree")
    @Operation(summary = "查询分类树")
    @PreAuthorize("@permission.hasPermission('content:category:query')")
    public Result<List<CategoryTreeVO>> listCategoryTree() {
        return Result.success(categoryAdminService.listCategoryTree());
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询分类详情")
    @PreAuthorize("@permission.hasPermission('content:category:query')")
    public Result<CategoryAdminVO> getCategory(@PathVariable Long id) {
        return Result.success(categoryAdminService.getCategory(id));
    }

    @PostMapping
    @Operation(summary = "新增分类")
    @PreAuthorize("@permission.hasPermission('content:category:create')")
    public Result<CategoryAdminVO> createCategory(@Valid @RequestBody CategorySaveRequest request) {
        return Result.success(categoryAdminService.createCategory(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改分类")
    @PreAuthorize("@permission.hasPermission('content:category:update')")
    public Result<CategoryAdminVO> updateCategory(@PathVariable Long id,
                                                  @Valid @RequestBody CategorySaveRequest request) {
        return Result.success(categoryAdminService.updateCategory(id, request));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "修改分类状态")
    @PreAuthorize("@permission.hasPermission('content:category:update')")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody StatusUpdateRequest request) {
        categoryAdminService.updateStatus(id, request.getStatus());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类")
    @PreAuthorize("@permission.hasPermission('content:category:delete')")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryAdminService.deleteCategory(id);
        return Result.success();
    }
}
