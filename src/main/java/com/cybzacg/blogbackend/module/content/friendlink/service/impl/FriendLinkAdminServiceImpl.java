package com.cybzacg.blogbackend.module.content.friendlink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.content.BlogFriendLink;
import com.cybzacg.blogbackend.dto.repository.content.BlogFriendLinkRepository;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.content.friendlink.convert.FriendLinkModelConvert;
import com.cybzacg.blogbackend.module.content.friendlink.model.admin.FriendLinkPageQuery;
import com.cybzacg.blogbackend.module.content.friendlink.model.admin.FriendLinkSaveRequest;
import com.cybzacg.blogbackend.module.content.friendlink.model.admin.FriendLinkVO;
import com.cybzacg.blogbackend.module.content.friendlink.service.FriendLinkAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendLinkAdminServiceImpl implements FriendLinkAdminService {
    private final BlogFriendLinkRepository friendLinkRepository;
    private final FriendLinkModelConvert friendLinkModelConvert;

    @Override
    public PageResult<FriendLinkVO> page(FriendLinkPageQuery query) {
        LambdaQueryWrapper<BlogFriendLink> wrapper = new LambdaQueryWrapper<BlogFriendLink>()
                .like(StrUtils.hasText(query.getName()), BlogFriendLink::getName, query.getName())
                .eq(query.getStatus() != null, BlogFriendLink::getStatus, query.getStatus())
                .orderByAsc(BlogFriendLink::getSortOrder)
                .orderByDesc(BlogFriendLink::getId);
        Page<BlogFriendLink> page = friendLinkRepository.page(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<FriendLinkVO> records = page.getRecords().stream()
                .map(friendLinkModelConvert::toVO)
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public FriendLinkVO getById(Long id) {
        return friendLinkModelConvert.toVO(getOrThrow(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FriendLinkVO create(FriendLinkSaveRequest request) {
        BlogFriendLink link = friendLinkModelConvert.toEntity(request);
        link.setSortOrder(link.getSortOrder() == null ? 0 : link.getSortOrder());
        link.setStatus(1);
        friendLinkRepository.save(link);
        return friendLinkModelConvert.toVO(link);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FriendLinkVO update(Long id, FriendLinkSaveRequest request) {
        BlogFriendLink link = getOrThrow(id);
        friendLinkModelConvert.updateEntity(request, link);
        friendLinkRepository.updateById(link);
        return friendLinkModelConvert.toVO(link);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        BlogFriendLink link = getOrThrow(id);
        link.setStatus(status);
        friendLinkRepository.updateById(link);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getOrThrow(id);
        friendLinkRepository.removeById(id);
    }

    private BlogFriendLink getOrThrow(Long id) {
        BlogFriendLink link = friendLinkRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(link, ResultErrorCode.ILLEGAL_ARGUMENT, "友情链接不存在");
        return link;
    }
}
