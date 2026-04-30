package com.cybzacg.blogbackend.module.content.footprint.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.content.SysUserFootprint;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.content.shared.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.footprint.model.admin.FootprintPageQuery;
import com.cybzacg.blogbackend.module.content.footprint.model.admin.FootprintVO;
import com.cybzacg.blogbackend.module.content.footprint.repository.SysUserFootprintRepository;
import com.cybzacg.blogbackend.module.content.footprint.service.FootprintAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户足迹后台管理服务实现。
 *
 * <p>负责后台足迹分页查询、单条删除以及按条件批量清理。
 */
@Service
@RequiredArgsConstructor
public class FootprintAdminServiceImpl implements FootprintAdminService {
    private final SysUserFootprintRepository sysUserFootprintRepository;
    private final ContentModelMapper contentModelMapper;

    /**
     * 按管理端条件分页查询用户足迹列表。
     */
    @Override
    public PageResult<FootprintVO> pageFootprints(FootprintPageQuery query) {
        Page<SysUserFootprint> page = sysUserFootprintRepository.pageByAdminConditions(query);
        List<FootprintVO> records = page.getRecords().stream()
                .map(contentModelMapper::toFootprintVO)
                .toList();
        return PageResult.of(page, records);
    }

    /**
     * 删除单条足迹记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFootprint(Long id) {
        SysUserFootprint footprint = sysUserFootprintRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(footprint, ResultErrorCode.ILLEGAL_ARGUMENT, "足迹不存在");
        sysUserFootprintRepository.removeById(id);
    }

    /**
     * 按管理端查询条件批量清理足迹记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanFootprints(FootprintPageQuery query) {
        sysUserFootprintRepository.removeByAdminConditions(query);
    }
}
