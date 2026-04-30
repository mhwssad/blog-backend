package com.cybzacg.blogbackend.module.auth.author.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.auth.SysAuthorApplication;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.enums.auth.AuthorApplicationStatusEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.author.convert.AuthorApplicationModelMapper;
import com.cybzacg.blogbackend.module.auth.author.model.admin.SysAuthorApplicationAdminPageQuery;
import com.cybzacg.blogbackend.module.auth.author.model.admin.SysAuthorApplicationRepairRequest;
import com.cybzacg.blogbackend.module.auth.author.model.admin.SysAuthorApplicationAdminReviewRequest;
import com.cybzacg.blogbackend.module.auth.author.model.admin.SysAuthorApplicationAdminVO;
import com.cybzacg.blogbackend.module.auth.author.repository.SysAuthorApplicationRepository;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.author.service.AuthorPermissionService;
import com.cybzacg.blogbackend.module.auth.author.service.SysAuthorApplicationAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 作者申请后台管理服务实现。
 */
@Service
@RequiredArgsConstructor
public class SysAuthorApplicationAdminServiceImpl implements SysAuthorApplicationAdminService {
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;

    private final SysAuthorApplicationRepository sysAuthorApplicationRepository;
    private final SysUserRepository sysUserRepository;
    private final AuthorPermissionService authorPermissionService;
    private final AuthorApplicationModelMapper authorApplicationModelMapper;

    /**
     * 分页查询作者申请记录，并补齐申请人与审核人展示信息。
     */
    @Override
    public PageResult<SysAuthorApplicationAdminVO> pageApplications(SysAuthorApplicationAdminPageQuery query) {
        SysAuthorApplicationAdminPageQuery safeQuery = normalizeQuery(query);
        Page<SysAuthorApplication> page = sysAuthorApplicationRepository.pageByAdminConditions(safeQuery);
        Map<Long, SysUser> userMap = loadUserMap(page.getRecords());
        List<SysAuthorApplicationAdminVO> records = page.getRecords().stream()
                .map(application -> authorApplicationModelMapper.toAdminVO(
                        application,
                        userMap.get(application.getUserId()),
                        userMap.get(application.getReviewerId())
                ))
                .toList();
        return PageResult.of(page, records);
    }

    /**
     * 查询单条作者申请详情。
     */
    @Override
    public SysAuthorApplicationAdminVO getApplication(Long id) {
        SysAuthorApplication application = requireApplication(id);
        Map<Long, SysUser> userMap = loadUserMap(List.of(application));
        return authorApplicationModelMapper.toAdminVO(
                application,
                userMap.get(application.getUserId()),
                userMap.get(application.getReviewerId())
        );
    }

    /**
     * 审核作者申请，只允许处理待审核状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewApplication(Long id, SysAuthorApplicationAdminReviewRequest request) {
        validateReviewRequest(request);
        SysAuthorApplication application = requireApplication(id);
        ExceptionThrowerCore.throwBusinessIf(
                !Objects.equals(application.getApplyStatus(), AuthorApplicationStatusEnum.PENDING.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "当前申请状态不可审核"
        );

        application.setApplyStatus(request.getReviewStatus());
        application.setReviewComment(trimToNull(request.getReviewComment()));
        application.setReviewerId(SecurityUtils.requireUserId());
        application.setReviewedAt(LocalDateTime.now());
        sysAuthorApplicationRepository.updateById(application);

        if (Objects.equals(request.getReviewStatus(), AuthorApplicationStatusEnum.APPROVED.getValue())) {
            authorPermissionService.grantAuthorRole(application.getUserId());
        }
    }

    /**
     * 修正作者申请状态，并同步作者角色到目标状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void repairApplication(Long id, SysAuthorApplicationRepairRequest request) {
        validateRepairRequest(request);
        SysAuthorApplication application = requireApplication(id);
        ExceptionThrowerCore.throwBusinessIf(
                Objects.equals(application.getApplyStatus(), request.getTargetStatus()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "目标状态与当前状态一致，无需修正"
        );

        application.setApplyStatus(request.getTargetStatus());
        application.setReviewComment(trimToNull(request.getReviewComment()));
        application.setReviewerId(SecurityUtils.requireUserId());
        application.setReviewedAt(LocalDateTime.now());
        sysAuthorApplicationRepository.updateById(application);
        syncAuthorRoleForStatus(application.getUserId(), request.getTargetStatus());
    }

    private SysAuthorApplication requireApplication(Long id) {
        SysAuthorApplication application = sysAuthorApplicationRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIf(application == null, ResultErrorCode.ILLEGAL_ARGUMENT, "作者申请不存在");
        return application;
    }

    private void validateReviewRequest(SysAuthorApplicationAdminReviewRequest request) {
        ExceptionThrowerCore.throwBusinessIfNull(request, ResultErrorCode.ILLEGAL_ARGUMENT, "审核参数不能为空");
        Integer reviewStatus = request.getReviewStatus();
        ExceptionThrowerCore.throwBusinessIf(
                !Objects.equals(reviewStatus, AuthorApplicationStatusEnum.APPROVED.getValue())
                        && !Objects.equals(reviewStatus, AuthorApplicationStatusEnum.REJECTED.getValue())
                        && !Objects.equals(reviewStatus, AuthorApplicationStatusEnum.NEED_MORE_INFO.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "审核状态不合法"
        );
    }

    private void validateRepairRequest(SysAuthorApplicationRepairRequest request) {
        ExceptionThrowerCore.throwBusinessIfNull(request, ResultErrorCode.ILLEGAL_ARGUMENT, "修正参数不能为空");
        ExceptionThrowerCore.throwBusinessIf(
                !AuthorApplicationStatusEnum.contains(request.getTargetStatus()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "目标状态不合法"
        );
        ExceptionThrowerCore.throwBusinessIf(
                trimToNull(request.getReviewComment()) == null,
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "修正作者申请状态必须填写备注"
        );
    }

    private SysAuthorApplicationAdminPageQuery normalizeQuery(SysAuthorApplicationAdminPageQuery query) {
        SysAuthorApplicationAdminPageQuery safeQuery = query == null ? new SysAuthorApplicationAdminPageQuery() : query;
        safeQuery.setCurrent(PaginationUtils.normalizeCurrent(safeQuery.getCurrent()));
        safeQuery.setSize(PaginationUtils.normalizeSize(safeQuery.getSize(), DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE));
        return safeQuery;
    }

    private Map<Long, SysUser> loadUserMap(List<SysAuthorApplication> applications) {
        if (applications == null || applications.isEmpty()) {
            return Map.of();
        }
        Set<Long> userIds = applications.stream()
                .flatMap(application -> Arrays.stream(new Long[]{application.getUserId(), application.getReviewerId()}))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return sysUserRepository.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (left, right) -> left));
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private void syncAuthorRoleForStatus(Long userId, Integer applyStatus) {
        if (Objects.equals(applyStatus, AuthorApplicationStatusEnum.APPROVED.getValue())) {
            authorPermissionService.grantAuthorRole(userId);
            return;
        }
        authorPermissionService.revokeAuthorRole(userId);
    }
}
