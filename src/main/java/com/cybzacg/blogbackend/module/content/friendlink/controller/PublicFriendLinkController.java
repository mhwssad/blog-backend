package com.cybzacg.blogbackend.module.content.friendlink.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.content.friendlink.model.publics.PublicFriendLinkVO;
import com.cybzacg.blogbackend.module.content.friendlink.service.PublicFriendLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/friend-links")
@Tag(name = "公开友情链接")
@RequiredArgsConstructor
public class PublicFriendLinkController {
    private final PublicFriendLinkService publicFriendLinkService;

    @GetMapping
    @Operation(summary = "查询启用友情链接列表")
    public Result<List<PublicFriendLinkVO>> listEnabled() {
        return Result.success(publicFriendLinkService.listEnabled());
    }
}
