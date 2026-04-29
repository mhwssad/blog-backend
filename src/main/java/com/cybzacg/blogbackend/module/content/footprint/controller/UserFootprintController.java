package com.cybzacg.blogbackend.module.content.footprint.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.content.footprint.model.user.UserFootprintPageQuery;
import com.cybzacg.blogbackend.module.content.footprint.model.user.UserFootprintVO;
import com.cybzacg.blogbackend.module.content.footprint.service.UserFootprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户足迹控制器。
 *
 * <p>负责对外暴露用户足迹相关接口，并作为请求进入业务层的统一入口。
 */
@RestController
@RequestMapping("/api/user/footprints")
@Tag(name = "用户足迹")
@RequiredArgsConstructor
public class UserFootprintController {
    private final UserFootprintService userFootprintService;

    @GetMapping
    @Operation(summary = "查询我的足迹")
    public Result<PageResult<UserFootprintVO>> pageFootprints(UserFootprintPageQuery query) {
        return Result.success(userFootprintService.pageFootprints(query));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除我的足迹")
    public Result<Void> deleteFootprint(@PathVariable Long id) {
        userFootprintService.deleteFootprint(id);
        return Result.success();
    }

    @DeleteMapping
    @Operation(summary = "清空我的足迹")
    public Result<Void> clearFootprints() {
        userFootprintService.clearFootprints();
        return Result.success();
    }
}
