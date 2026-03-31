package com.cybzacg.blogbackend.module.follow;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.mapper.SysUserFollowMapper;
import com.cybzacg.blogbackend.module.follow.convert.FollowModelMapper;
import com.cybzacg.blogbackend.module.follow.model.admin.FollowAdminPageQuery;
import com.cybzacg.blogbackend.module.follow.model.admin.FollowAdminRelationVO;
import com.cybzacg.blogbackend.module.follow.model.admin.FollowRelationCleanRequest;
import com.cybzacg.blogbackend.module.follow.model.data.FollowAdminRelationItem;
import com.cybzacg.blogbackend.module.follow.service.impl.FollowAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowAdminServiceImplTest {
    @Mock
    private SysUserFollowMapper sysUserFollowMapper;
    @Mock
    private FollowModelMapper followModelMapper;

    private FollowAdminServiceImpl followAdminService;

    @BeforeEach
    void setUp() {
        followAdminService = new FollowAdminServiceImpl(sysUserFollowMapper, followModelMapper);
    }

    @Test
    void pageRelationsShouldReturnMappedRecords() {
        FollowAdminPageQuery query = new FollowAdminPageQuery();
        query.setCurrent(3L);
        query.setSize(2L);
        when(sysUserFollowMapper.countAdminRelationPage(query)).thenReturn(5L);

        FollowAdminRelationItem item = new FollowAdminRelationItem();
        item.setRelationId(9L);
        item.setFollowerId(2L);
        FollowAdminRelationVO vo = new FollowAdminRelationVO();
        vo.setRelationId(9L);
        vo.setFollowerId(2L);
        when(sysUserFollowMapper.selectAdminRelationPage(query, 4L, 2L)).thenReturn(List.of(item));
        when(followModelMapper.toFollowAdminRelationVO(item)).thenReturn(vo);

        PageResult<FollowAdminRelationVO> result = followAdminService.pageRelations(query);

        assertEquals(5L, result.getTotal());
        assertEquals(3L, result.getCurrent());
        assertEquals(2L, result.getSize());
        assertEquals(1, result.getRecords().size());
        assertEquals(9L, result.getRecords().get(0).getRelationId());
    }

    @Test
    void cleanRelationsShouldDeleteOnlyWhenRecordsExist() {
        FollowRelationCleanRequest request = new FollowRelationCleanRequest();
        request.setCleanInactive(true);
        request.setCleanDeletedUsers(false);
        request.setCleanDisabledUsers(false);
        when(sysUserFollowMapper.countCleanableRelations(true, false, false)).thenReturn(3L);

        long result = followAdminService.cleanRelations(request);

        assertEquals(3L, result);
        verify(sysUserFollowMapper).deleteCleanableRelations(true, false, false);
    }

    @Test
    void cleanRelationsShouldSkipDeleteWhenNoRecordsExist() {
        FollowRelationCleanRequest request = new FollowRelationCleanRequest();
        request.setCleanInactive(false);
        request.setCleanDeletedUsers(true);
        request.setCleanDisabledUsers(false);
        when(sysUserFollowMapper.countCleanableRelations(false, true, false)).thenReturn(0L);

        long result = followAdminService.cleanRelations(request);

        assertEquals(0L, result);
        verify(sysUserFollowMapper, never()).deleteCleanableRelations(false, true, false);
    }
}
