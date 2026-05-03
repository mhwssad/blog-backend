package com.cybzacg.blogbackend.module.auth.author.service.impl;

import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.article.model.internal.AuthorPublicProfileStats;
import com.cybzacg.blogbackend.module.article.service.ArticleProfileQueryService;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.author.convert.PublicAuthorProfileModelConvert;
import com.cybzacg.blogbackend.module.auth.author.model.publics.PublicAuthorProfileVO;
import com.cybzacg.blogbackend.module.auth.author.service.AuthorPermissionService;
import com.cybzacg.blogbackend.module.auth.author.service.PublicAuthorProfileService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 公开作者主页服务实现。
 */
@Service
@RequiredArgsConstructor
public class PublicAuthorProfileServiceImpl implements PublicAuthorProfileService {
    private static final String AUTHOR_BADGE = "author";

    private final SysUserRepository sysUserRepository;
    private final AuthorPermissionService authorPermissionService;
    private final ArticleProfileQueryService articleProfileQueryService;
    private final PublicAuthorProfileModelConvert publicAuthorProfileModelConvert;

    /**
     * {@inheritDoc}
     */
    @Override
    public PublicAuthorProfileVO getAuthorProfile(Long userId) {
        SysUser user = requireActiveUser(userId);
        PublicAuthorProfileVO vo = publicAuthorProfileModelConvert.toPublicAuthorProfileVO(user);
        boolean isAuthor = authorPermissionService.hasAuthorRole(userId);
        vo.setAuthor(isAuthor);
        vo.setAuthorBadge(isAuthor ? AUTHOR_BADGE : null);
        AuthorPublicProfileStats stats = articleProfileQueryService.getAuthorPublicProfileStats(userId);
        vo.setPublicArticleCount(stats.getPublicArticleCount());
        vo.setPublicSeriesCount(stats.getPublicSeriesCount());
        return vo;
    }

    /**
     * 校验公开主页目标用户存在且处于可公开访问状态。
     */
    private SysUser requireActiveUser(Long userId) {
        ExceptionThrowerCore.throwBusinessIfNull(userId, ResultErrorCode.USER_NOT_FOUND, "用户不存在");
        SysUser user = sysUserRepository.getById(userId);
        ExceptionThrowerCore.throwBusinessIf(
                user == null || !Objects.equals(user.getDeletedFlag(), 0) || !Objects.equals(user.getStatus(), 1),
                ResultErrorCode.USER_NOT_FOUND,
                "用户不存在"
        );
        return user;
    }
}
