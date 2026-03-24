package com.cybzacg.blogbackend.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysUserFootprint;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.admin.FootprintPageQuery;
import com.cybzacg.blogbackend.module.content.model.admin.FootprintVO;
import com.cybzacg.blogbackend.module.content.service.FootprintAdminService;
import com.cybzacg.blogbackend.module.content.service.SysUserFootprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FootprintAdminServiceImpl implements FootprintAdminService {
    private final SysUserFootprintService sysUserFootprintService;
    private final ContentModelMapper contentModelMapper;

    @Override
    public PageResult<FootprintVO> pageFootprints(FootprintPageQuery query) {
        LambdaQueryWrapper<SysUserFootprint> wrapper = buildWrapper(query)
                .orderByDesc(SysUserFootprint::getVisitedAt)
                .orderByDesc(SysUserFootprint::getId);
        Page<SysUserFootprint> page = sysUserFootprintService.page(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<FootprintVO> records = page.getRecords().stream()
                .map(contentModelMapper::toFootprintVO)
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFootprint(Long id) {
        SysUserFootprint footprint = sysUserFootprintService.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(footprint, ResultErrorCode.ILLEGAL_ARGUMENT, "足迹不存在");
        sysUserFootprintService.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanFootprints(FootprintPageQuery query) {
        sysUserFootprintService.remove(buildWrapper(query));
    }

    private LambdaQueryWrapper<SysUserFootprint> buildWrapper(FootprintPageQuery query) {
        return new LambdaQueryWrapper<SysUserFootprint>()
                .eq(query.getUserId() != null, SysUserFootprint::getUserId, query.getUserId())
                .eq(query.getTargetId() != null, SysUserFootprint::getTargetId, query.getTargetId())
                .eq(query.getTargetType() != null, SysUserFootprint::getTargetType, query.getTargetType())
                .ge(query.getVisitedAtStart() != null, SysUserFootprint::getVisitedAt, query.getVisitedAtStart())
                .le(query.getVisitedAtEnd() != null, SysUserFootprint::getVisitedAt, query.getVisitedAtEnd());
    }
}


