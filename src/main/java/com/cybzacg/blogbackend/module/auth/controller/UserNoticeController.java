package com.cybzacg.blogbackend.module.auth.controller;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.model.admin.UserNoticePageQuery;
import com.cybzacg.blogbackend.module.auth.model.admin.UserNoticeVO;
import com.cybzacg.blogbackend.module.auth.service.UserNoticeInboxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户通知控制器。
 *
 * <p>负责对外暴露用户通知相关接口，并作为请求进入业务层的统一入口。
 */
@RestController
@RequestMapping("/api/user/notices")
@Tag(name = "用户通知中心")
@RequiredArgsConstructor
public class UserNoticeController {
    private final UserNoticeInboxService userNoticeInboxService;

    @GetMapping
    @Operation(summary = "我的通知列表")
    public Result<PageResult<UserNoticeVO>> pageMyNotices(UserNoticePageQuery query) {
        return Result.success(userNoticeInboxService.pageMyNotices(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "我的通知详情")
    public Result<UserNoticeVO> getMyNotice(@PathVariable Long id) {
        return Result.success(userNoticeInboxService.getMyNotice(id));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "我的未读数")
    public Result<Long> countUnreadNotices() {
        return Result.success(userNoticeInboxService.countUnreadNotices());
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "单条已读")
    public Result<Void> markRead(@PathVariable Long id) {
        userNoticeInboxService.markRead(id);
        return Result.success();
    }

    @PostMapping("/read-all")
    @Operation(summary = "全部已读")
    public Result<Void> markAllRead() {
        userNoticeInboxService.markAllRead();
        return Result.success();
    }
}
