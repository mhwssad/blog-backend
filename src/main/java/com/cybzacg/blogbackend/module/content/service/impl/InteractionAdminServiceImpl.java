package com.cybzacg.blogbackend.module.content.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.BlogArticle;
import com.cybzacg.blogbackend.domain.SysComment;
import com.cybzacg.blogbackend.domain.SysInteraction;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleRepository;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.admin.InteractionPageQuery;
import com.cybzacg.blogbackend.module.content.model.admin.InteractionVO;
import com.cybzacg.blogbackend.module.content.repository.SysCommentRepository;
import com.cybzacg.blogbackend.module.content.repository.SysInteractionRepository;
import com.cybzacg.blogbackend.module.content.service.InteractionAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InteractionAdminServiceImpl implements InteractionAdminService {
    private final SysInteractionRepository sysInteractionRepository;
    private final BlogArticleRepository blogArticleService;
    private final SysCommentRepository sysCommentRepository;
    private final ContentModelMapper contentModelMapper;

    @Override
    public PageResult<InteractionVO> pageInteractions(InteractionPageQuery query) {
        Page<SysInteraction> page = sysInteractionRepository.pageByAdminConditions(query);
        List<InteractionVO> records = page.getRecords().stream()
                .map(contentModelMapper::toInteractionVO)
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInteraction(Long id) {
        SysInteraction interaction = sysInteractionRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(interaction, ResultErrorCode.ILLEGAL_ARGUMENT, "互动记录不存在");
        if ("article".equals(interaction.getTargetType())) {
            BlogArticle article = blogArticleService.getById(interaction.getTargetId());
            if (article != null) {
                article.setLikeCount(Math.max(0, (article.getLikeCount() == null ? 0 : article.getLikeCount()) - 1));
                blogArticleService.updateById(article);
            }
        } else if ("comment".equals(interaction.getTargetType())) {
            SysComment comment = sysCommentRepository.getById(interaction.getTargetId());
            if (comment != null) {
                comment.setLikeCount(Math.max(0, (comment.getLikeCount() == null ? 0 : comment.getLikeCount()) - 1));
                sysCommentRepository.updateById(comment);
            }
        }
        sysInteractionRepository.removeById(id);
    }
}

