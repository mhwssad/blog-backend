package com.cybzacg.blogbackend.module.auth.author.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.author.model.publics.PublicAuthorProfileVO;
import com.cybzacg.blogbackend.module.auth.author.service.PublicAuthorProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开作者主页控制器。
 */
@RestController
@RequestMapping("/api/users/{userId}")
@Tag(name = "公开作者主页")
@RequiredArgsConstructor
public class PublicAuthorProfileController {
    private final PublicAuthorProfileService publicAuthorProfileService;

    @GetMapping("/author-profile")
    @Operation(summary = "查询指定用户的公开作者主页摘要")
    public Result<PublicAuthorProfileVO> getAuthorProfile(@PathVariable Long userId) {
        return Result.success(publicAuthorProfileService.getAuthorProfile(userId));
    }
}
