package com.cybzacg.blogbackend.module.content.interaction.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.content.interaction.model.admin.InteractionPageQuery;
import com.cybzacg.blogbackend.module.content.interaction.model.admin.InteractionVO;
import com.cybzacg.blogbackend.module.content.interaction.service.InteractionAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Content互动后台管理控制器。
 *
 * <p>负责对外暴露Content互动后台管理相关接口，并作为请求进入业务层的统一入口。
 */
@RestController
@RequestMapping("/api/sys/interactions")
@Tag(name = "后台互动管理")
@RequiredArgsConstructor
public class ContentInteractionAdminController {
    private final InteractionAdminService interactionAdminService;

    @GetMapping
    @Operation(summary = "分页查询互动")
    @PreAuthorize("@permission.hasPermission('content:interaction:query')")
    public Result<PageResult<InteractionVO>> pageInteractions(InteractionPageQuery query) {
        return Result.success(interactionAdminService.pageInteractions(query));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除互动")
    @PreAuthorize("@permission.hasPermission('content:interaction:delete')")
    public Result<Void> deleteInteraction(@PathVariable Long id) {
        interactionAdminService.deleteInteraction(id);
        return Result.success();
    }
}
