package com.cybzacg.blogbackend.module.content.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysUserFootprint;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.admin.FootprintPageQuery;
import com.cybzacg.blogbackend.module.content.model.admin.FootprintVO;
import com.cybzacg.blogbackend.module.content.repository.SysUserFootprintRepository;
import com.cybzacg.blogbackend.module.content.service.FootprintAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FootprintAdminServiceImpl implements FootprintAdminService {
    private final SysUserFootprintRepository sysUserFootprintRepository;
    private final ContentModelMapper contentModelMapper;

    @Override
    public PageResult<FootprintVO> pageFootprints(FootprintPageQuery query) {
        Page<SysUserFootprint> page = sysUserFootprintRepository.pageByAdminConditions(query);
        List<FootprintVO> records = page.getRecords().stream()
                .map(contentModelMapper::toFootprintVO)
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFootprint(Long id) {
        SysUserFootprint footprint = sysUserFootprintRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(footprint, ResultErrorCode.ILLEGAL_ARGUMENT, "足迹不存在");
        sysUserFootprintRepository.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanFootprints(FootprintPageQuery query) {
        sysUserFootprintRepository.removeByAdminConditions(query);
    }
}
