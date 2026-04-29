package com.cybzacg.blogbackend.module.content.interaction.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysComment;
import com.cybzacg.blogbackend.domain.SysInteraction;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.article.service.ArticleContentFacadeService;
import com.cybzacg.blogbackend.module.content.shared.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.interaction.model.admin.InteractionPageQuery;
import com.cybzacg.blogbackend.module.content.interaction.model.admin.InteractionVO;
import com.cybzacg.blogbackend.module.content.comment.repository.SysCommentRepository;
import com.cybzacg.blogbackend.module.content.interaction.repository.SysInteractionRepository;
import com.cybzacg.blogbackend.module.content.interaction.service.InteractionAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 互动记录后台管理服务实现。
 *
 * <p>负责后台互动记录分页查询与删除，删除时同步回退对应目标的点赞计数。
 */
@Service
@RequiredArgsConstructor
public class InteractionAdminServiceImpl implements InteractionAdminService {
    private final SysInteractionRepository sysInteractionRepository;
    private final ArticleContentFacadeService articleContentFacadeService;
    private final SysCommentRepository sysCommentRepository;
    private final ContentModelMapper contentModelMapper;

    /**
     * 按管理端条件分页查询互动记录列表。
     */
    @Override
    public PageResult<InteractionVO> pageInteractions(InteractionPageQuery query) {
        Page<SysInteraction> page = sysInteractionRepository.pageByAdminConditions(query);
        List<InteractionVO> records = page.getRecords().stream()
                .map(contentModelMapper::toInteractionVO)
                .toList();
        return PageResult.of(page, records);
    }

    /**
     * 删除互动记录，并根据目标类型回退文章或评论的点赞计数。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInteraction(Long id) {
        SysInteraction interaction = sysInteractionRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(interaction, ResultErrorCode.ILLEGAL_ARGUMENT, "互动记录不存在");
        if ("article".equals(interaction.getTargetType())) {
            articleContentFacadeService.adjustLikeCount(interaction.getTargetId(), -1);
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
