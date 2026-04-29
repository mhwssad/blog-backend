package com.cybzacg.blogbackend.module.content.footprint.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.content.footprint.model.admin.FootprintPageQuery;
import com.cybzacg.blogbackend.module.content.footprint.model.admin.FootprintVO;
import com.cybzacg.blogbackend.module.content.footprint.service.FootprintAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Content足迹后台管理控制器。
 *
 * <p>负责对外暴露Content足迹后台管理相关接口，并作为请求进入业务层的统一入口。
 */
@RestController
@RequestMapping("/api/sys/footprints")
@Tag(name = "后台足迹管理")
@RequiredArgsConstructor
public class ContentFootprintAdminController {
    private final FootprintAdminService footprintAdminService;

    @GetMapping
    @Operation(summary = "分页查询足迹")
    @PreAuthorize("@permission.hasPermission('content:footprint:query')")
    public Result<PageResult<FootprintVO>> pageFootprints(FootprintPageQuery query) {
        return Result.success(footprintAdminService.pageFootprints(query));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除足迹")
    @PreAuthorize("@permission.hasPermission('content:footprint:delete')")
    public Result<Void> deleteFootprint(@PathVariable Long id) {
        footprintAdminService.deleteFootprint(id);
        return Result.success();
    }

    @DeleteMapping
    @Operation(summary = "按条件清理足迹")
    @PreAuthorize("@permission.hasPermission('content:footprint:delete')")
    public Result<Void> cleanFootprints(FootprintPageQuery query) {
        footprintAdminService.cleanFootprints(query);
        return Result.success();
    }
}
