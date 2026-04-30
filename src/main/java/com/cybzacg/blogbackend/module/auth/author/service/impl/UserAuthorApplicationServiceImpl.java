package com.cybzacg.blogbackend.module.auth.author.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.auth.SysAuthorApplication;
import com.cybzacg.blogbackend.domain.auth.SysUser;
import com.cybzacg.blogbackend.enums.auth.AuthorApplicationStatusEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.author.convert.AuthorApplicationModelMapper;
import com.cybzacg.blogbackend.module.auth.author.model.user.UserAuthorApplicationPageQuery;
import com.cybzacg.blogbackend.module.auth.author.model.user.UserAuthorApplicationSubmitRequest;
import com.cybzacg.blogbackend.module.auth.author.model.user.UserAuthorApplicationVO;
import com.cybzacg.blogbackend.module.auth.author.repository.SysAuthorApplicationRepository;
import com.cybzacg.blogbackend.module.auth.account.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.author.service.AuthorPermissionService;
import com.cybzacg.blogbackend.module.auth.author.service.UserAuthorApplicationService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 用户作者申请服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserAuthorApplicationServiceImpl implements UserAuthorApplicationService {
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;

    private final SysAuthorApplicationRepository sysAuthorApplicationRepository;
    private final SysUserRepository sysUserRepository;
    private final AuthorPermissionService authorPermissionService;
    private final AuthorApplicationModelMapper authorApplicationModelMapper;

    /**
     * 提交或补充作者申请；当最近申请为“待补充”时复用原记录回到待审核状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAuthorApplicationVO submitApplication(UserAuthorApplicationSubmitRequest request) {
        Long userId = SecurityUtils.requireUserId();
        requireSubmittableUser(userId);
        ExceptionThrowerCore.throwBusinessIf(
                authorPermissionService.hasAuthorRole(userId),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "当前用户已具备作者权限，无需重复申请"
        );
        SysAuthorApplication latest = sysAuthorApplicationRepository.findLatestByUserId(userId);
        if (latest != null) {
            if (Objects.equals(latest.getApplyStatus(), AuthorApplicationStatusEnum.PENDING.getValue())) {
                ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.ILLEGAL_ARGUMENT, "当前已有待审核申请，请勿重复提交");
            }
            if (Objects.equals(latest.getApplyStatus(), AuthorApplicationStatusEnum.APPROVED.getValue())) {
                ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.ILLEGAL_ARGUMENT, "作者申请已通过，无需重复提交");
            }
            if (Objects.equals(latest.getApplyStatus(), AuthorApplicationStatusEnum.NEED_MORE_INFO.getValue())) {
                return resubmitLatestApplication(latest, request);
            }
        }
        LocalDateTime now = LocalDateTime.now();
        SysAuthorApplication application = authorApplicationModelMapper.toApplication(request);
        application.setUserId(userId);
        application.setApplyStatus(AuthorApplicationStatusEnum.PENDING.getValue());
        application.setSubmittedAt(now);
        sysAuthorApplicationRepository.save(application);
        return authorApplicationModelMapper.toUserVO(application);
    }

    /**
     * 查询当前用户最近一次作者申请状态。
     */
    @Override
    public UserAuthorApplicationVO getLatestApplication() {
        Long userId = SecurityUtils.requireUserId();
        SysAuthorApplication application = sysAuthorApplicationRepository.findLatestByUserId(userId);
        return application == null ? null : authorApplicationModelMapper.toUserVO(application);
    }

    /**
     * 分页查询当前用户的作者申请记录。
     */
    @Override
    public PageResult<UserAuthorApplicationVO> pageMyApplications(UserAuthorApplicationPageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        long current = PaginationUtils.normalizeCurrent(query == null ? null : query.getCurrent());
        long size = PaginationUtils.normalizeSize(query == null ? null : query.getSize(), DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
        Page<SysAuthorApplication> page = sysAuthorApplicationRepository.pageByUserId(userId, current, size);
        List<UserAuthorApplicationVO> records = page.getRecords().stream()
                .map(authorApplicationModelMapper::toUserVO)
                .toList();
        return PageResult.of(page, records);
    }

    private UserAuthorApplicationVO resubmitLatestApplication(SysAuthorApplication latest,
                                                              UserAuthorApplicationSubmitRequest request) {
        authorApplicationModelMapper.updateApplication(request, latest);
        latest.setApplyStatus(AuthorApplicationStatusEnum.PENDING.getValue());
        latest.setReviewerId(null);
        latest.setReviewComment(null);
        latest.setReviewedAt(null);
        latest.setSubmittedAt(LocalDateTime.now());
        sysAuthorApplicationRepository.updateById(latest);
        return authorApplicationModelMapper.toUserVO(latest);
    }

    private void requireSubmittableUser(Long userId) {
        SysUser user = sysUserRepository.getById(userId);
        ExceptionThrowerCore.throwBusinessIf(
                user == null || Integer.valueOf(1).equals(user.getDeletedFlag()),
                ResultErrorCode.USER_NOT_FOUND
        );
        ExceptionThrowerCore.throwBusinessIf(
                !Objects.equals(user.getStatus(), 1),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "当前账号不可提交作者申请"
        );
    }
}
