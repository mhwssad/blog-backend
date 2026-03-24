package com.cybzacg.blogbackend.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysComment;
import com.cybzacg.blogbackend.domain.SysInteraction;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.module.article.service.BlogArticleService;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.admin.InteractionPageQuery;
import com.cybzacg.blogbackend.module.content.model.admin.InteractionVO;
import com.cybzacg.blogbackend.module.content.service.InteractionAdminService;
import com.cybzacg.blogbackend.module.content.service.SysCommentService;
import com.cybzacg.blogbackend.module.content.service.SysInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InteractionAdminServiceImpl implements InteractionAdminService {
    private final SysInteractionService sysInteractionService;
    private final BlogArticleService blogArticleService;
    private final SysCommentService sysCommentService;
    private final ContentModelMapper contentModelMapper;

    @Override
    public PageResult<InteractionVO> pageInteractions(InteractionPageQuery query) {
        LambdaQueryWrapper<SysInteraction> wrapper = new LambdaQueryWrapper<SysInteraction>()
                .eq(query.getUserId() != null, SysInteraction::getUserId, query.getUserId())
                .eq(query.getTargetId() != null, SysInteraction::getTargetId, query.getTargetId())
                .eq(query.getTargetType() != null, SysInteraction::getTargetType, query.getTargetType())
                .eq(query.getActionType() != null, SysInteraction::getActionType, query.getActionType())
                .orderByDesc(SysInteraction::getCreatedAt)
                .orderByDesc(SysInteraction::getId);
        Page<SysInteraction> page = sysInteractionService.page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<InteractionVO> records = page.getRecords().stream()
                .map(contentModelMapper::toInteractionVO)
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInteraction(Long id) {
        SysInteraction interaction = sysInteractionService.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(interaction, ResultErrorCode.ILLEGAL_ARGUMENT, "互动记录不存在");
        if ("article".equals(interaction.getTargetType())) {
            BlogArticle article = blogArticleService.getById(interaction.getTargetId());
            if (article != null) {
                article.setLikeCount(Math.max(0, (article.getLikeCount() == null ? 0 : article.getLikeCount()) - 1));
                blogArticleService.updateById(article);
            }
        } else if ("comment".equals(interaction.getTargetType())) {
            SysComment comment = sysCommentService.getById(interaction.getTargetId());
            if (comment != null) {
                comment.setLikeCount(Math.max(0, (comment.getLikeCount() == null ? 0 : comment.getLikeCount()) - 1));
                sysCommentService.updateById(comment);
            }
        }
        sysInteractionService.removeById(id);
    }
}


