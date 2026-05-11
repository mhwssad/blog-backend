package com.cybzacg.blogbackend.module.forum.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.forum.ForumSection;
import com.cybzacg.blogbackend.dto.repository.forum.ForumPostRepository;
import com.cybzacg.blogbackend.dto.repository.forum.ForumSectionRepository;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.forum.ForumVisibilityScopeEnum;
import com.cybzacg.blogbackend.module.forum.convert.ForumModelConvert;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumSectionAdminVO;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumSectionPageQuery;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumSectionSaveRequest;
import com.cybzacg.blogbackend.module.forum.service.ForumSectionAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 论坛版块后台管理服务实现。
 *
 * <p>负责版块分页查询、维护、启停状态切换，以及删除前帖子引用校验。
 */
@Service
@RequiredArgsConstructor
public class ForumSectionAdminServiceImpl implements ForumSectionAdminService {
    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_ENABLED = 1;

    private final ForumSectionRepository forumSectionRepository;
    private final ForumPostRepository forumPostRepository;
    private final ForumModelConvert forumModelConvert;

    @Override
    public PageResult<ForumSectionAdminVO> pageSections(ForumSectionPageQuery query) {
        ForumSectionPageQuery safeQuery = normalizeQuery(query);
        Page<ForumSection> page = forumSectionRepository.pageAdminSections(safeQuery);
        return PageResult.of(page, page.getRecords().stream()
                .map(forumModelConvert::toSectionAdminVO)
                .toList());
    }

    @Override
    public ForumSectionAdminVO getSection(Long id) {
        return forumModelConvert.toSectionAdminVO(getSectionOrThrow(id));
    }

    /**
     * 创建论坛版块，统一处理裁剪、默认值和名称唯一性。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ForumSectionAdminVO createSection(ForumSectionSaveRequest request) {
        validateRequest(request, null);
        ForumSection section = forumModelConvert.toSection(request);
        applyDefaults(section);
        forumSectionRepository.save(section);
        return forumModelConvert.toSectionAdminVO(section);
    }

    /**
     * 更新论坛版块，保持名称唯一并复用保存请求的字段规范。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ForumSectionAdminVO updateSection(Long id, ForumSectionSaveRequest request) {
        ForumSection section = getSectionOrThrow(id);
        validateRequest(request, id);
        forumModelConvert.updateSection(request, section);
        applyDefaults(section);
        forumSectionRepository.updateById(section);
        return forumModelConvert.toSectionAdminVO(section);
    }

    /**
     * 启用或禁用论坛版块，只允许状态值 0/1。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        ForumSection section = getSectionOrThrow(id);
        ExceptionThrowerCore.throwBusinessIf(!isValidStatus(status), ResultErrorCode.ILLEGAL_ARGUMENT, "版块状态非法");
        section.setStatus(status);
        forumSectionRepository.updateById(section);
    }

    /**
     * 删除空版块；已存在帖子时必须保留归属关系，改用禁用。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSection(Long id) {
        getSectionOrThrow(id);
        boolean existsPost = forumPostRepository.existsBySectionId(id);
        ExceptionThrowerCore.throwBusinessIf(existsPost, ResultErrorCode.ILLEGAL_ARGUMENT, "当前版块已存在帖子，无法删除");
        forumSectionRepository.removeById(id);
    }

    private ForumSectionPageQuery normalizeQuery(ForumSectionPageQuery query) {
        ForumSectionPageQuery safeQuery = query == null ? new ForumSectionPageQuery() : query;
        safeQuery.setCurrent(PaginationUtils.normalizeCurrent(safeQuery.getCurrent()));
        safeQuery.setSize(PaginationUtils.normalizeSize(safeQuery.getSize(), 10L, 100L));
        safeQuery.setKeyword(StrUtils.trimToNull(safeQuery.getKeyword()));
        return safeQuery;
    }

    private void validateRequest(ForumSectionSaveRequest request, Long currentId) {
        String name = StrUtils.trim(request.getName());
        boolean duplicated = forumSectionRepository.existsByNameExcludingId(name, currentId);
        ExceptionThrowerCore.throwBusinessIf(duplicated, ResultErrorCode.DATA_ALREADY_EXISTS, "版块名称已存在");
    }

    private void applyDefaults(ForumSection section) {
        section.setSortOrder(section.getSortOrder() == null ? 0 : section.getSortOrder());
        section.setVisibilityScope(section.getVisibilityScope() == null
                ? ForumVisibilityScopeEnum.PUBLIC.getValue()
                : section.getVisibilityScope());
        section.setPostLevelLimit(section.getPostLevelLimit() == null ? 1 : section.getPostLevelLimit());
        section.setStatus(section.getStatus() == null ? STATUS_ENABLED : section.getStatus());
    }

    private boolean isValidStatus(Integer status) {
        return Objects.equals(status, STATUS_DISABLED) || Objects.equals(status, STATUS_ENABLED);
    }

    private ForumSection getSectionOrThrow(Long id) {
        ForumSection section = forumSectionRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(section, ResultErrorCode.ILLEGAL_ARGUMENT, "论坛版块不存在");
        return section;
    }
}
