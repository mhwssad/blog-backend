package com.cybzacg.blogbackend.module.auth.experience.controller;

import com.cybzacg.blogbackend.core.web.Result;
import com.cybzacg.blogbackend.module.auth.experience.model.user.UserLevelInfoVO;
import com.cybzacg.blogbackend.module.auth.experience.service.UserExperienceService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户侧经验与等级接口。
 */
@RestController
@RequestMapping("/api/user/experience")
@Tag(name = "用户经验等级")
@RequiredArgsConstructor
public class UserExperienceController {

    private final UserExperienceService userExperienceService;

    @GetMapping("/level")
    @Operation(summary = "查看当前等级信息")
    public Result<UserLevelInfoVO> getMyLevelInfo() {
        return Result.success(userExperienceService.getLevelInfo(SecurityUtils.requireUserId()));
    }
}
