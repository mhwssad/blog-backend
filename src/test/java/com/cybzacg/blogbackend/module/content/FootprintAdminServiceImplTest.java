package com.cybzacg.blogbackend.module.content;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysUserFootprint;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.admin.FootprintPageQuery;
import com.cybzacg.blogbackend.module.content.model.admin.FootprintVO;
import com.cybzacg.blogbackend.module.content.repository.SysUserFootprintRepository;
import com.cybzacg.blogbackend.module.content.service.impl.FootprintAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FootprintAdminServiceImplTest {
    @Mock
    private SysUserFootprintRepository sysUserFootprintRepository;
    @Mock
    private ContentModelMapper contentModelMapper;

    private FootprintAdminServiceImpl footprintAdminService;

    @BeforeEach
    void setUp() {
        footprintAdminService = new FootprintAdminServiceImpl(sysUserFootprintRepository, contentModelMapper);
    }

    @Test
    void pageFootprintsShouldReturnMappedRecords() {
        FootprintPageQuery query = new FootprintPageQuery();
        query.setCurrent(2L);
        query.setSize(6L);
        query.setUserId(7L);
        query.setTargetId(100L);
        query.setTargetType("article");
        query.setVisitedAtStart(new Date(1000L));
        query.setVisitedAtEnd(new Date(2000L));

        SysUserFootprint footprint = footprint(31L, 7L, 100L, "article");
        Page<SysUserFootprint> page = new Page<>(2, 6, 1);
        page.setRecords(List.of(footprint));

        FootprintVO vo = new FootprintVO();
        vo.setId(31L);
        vo.setTargetId(100L);

        when(sysUserFootprintRepository.pageByAdminConditions(query)).thenReturn(page);
        when(contentModelMapper.toFootprintVO(footprint)).thenReturn(vo);

        PageResult<FootprintVO> result = footprintAdminService.pageFootprints(query);

        assertEquals(1L, result.getTotal());
        assertEquals(2L, result.getCurrent());
        assertEquals(6L, result.getSize());
        assertEquals(1, result.getRecords().size());
        assertSame(vo, result.getRecords().get(0));
    }

    @Test
    void deleteFootprintShouldRemoveById() {
        SysUserFootprint footprint = footprint(31L, 7L, 100L, "article");
        when(sysUserFootprintRepository.getById(31L)).thenReturn(footprint);

        footprintAdminService.deleteFootprint(31L);

        verify(sysUserFootprintRepository).removeById(31L);
    }

    @Test
    void cleanFootprintsShouldRemoveByQueryWrapper() {
        FootprintPageQuery query = new FootprintPageQuery();
        query.setUserId(7L);
        query.setTargetType("article");
        query.setVisitedAtStart(new Date(1000L));
        query.setVisitedAtEnd(new Date(2000L));

        footprintAdminService.cleanFootprints(query);

        verify(sysUserFootprintRepository).removeByAdminConditions(query);
    }

    @Test
    void deleteFootprintShouldThrowWhenRecordMissing() {
        when(sysUserFootprintRepository.getById(31L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> footprintAdminService.deleteFootprint(31L));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("足迹不存在", exception.getMessage());
        verify(sysUserFootprintRepository, never()).removeById(31L);
    }

    private SysUserFootprint footprint(Long id, Long userId, Long targetId, String targetType) {
        SysUserFootprint footprint = new SysUserFootprint();
        footprint.setId(id);
        footprint.setUserId(userId);
        footprint.setTargetId(targetId);
        footprint.setTargetType(targetType);
        return footprint;
    }
}
