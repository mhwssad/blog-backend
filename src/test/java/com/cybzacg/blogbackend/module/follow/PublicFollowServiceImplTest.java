package com.cybzacg.blogbackend.module.follow;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysUser;
import com.cybzacg.blogbackend.module.auth.repository.SysUserRepository;
import com.cybzacg.blogbackend.module.follow.convert.FollowModelMapper;
import com.cybzacg.blogbackend.module.follow.model.data.PublicFollowUserItem;
import com.cybzacg.blogbackend.module.follow.model.publics.PublicFollowPageQuery;
import com.cybzacg.blogbackend.module.follow.model.publics.PublicFollowUserVO;
import com.cybzacg.blogbackend.module.follow.repository.SysUserFollowRepository;
import com.cybzacg.blogbackend.module.follow.service.impl.PublicFollowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicFollowServiceImplTest {
    @Mock
    private SysUserFollowRepository sysUserFollowRepository;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private FollowModelMapper followModelMapper;

    private PublicFollowServiceImpl publicFollowService;

    @BeforeEach
    void setUp() {
        publicFollowService = new PublicFollowServiceImpl(sysUserFollowRepository, sysUserRepository, followModelMapper);
    }

    @Test
    void pageUserFollowsShouldReturnMappedRecords() {
        PublicFollowPageQuery query = new PublicFollowPageQuery();
        query.setCurrent(2L);
        query.setSize(5L);
        when(sysUserRepository.getById(12L)).thenReturn(activeUser(12L));
        when(sysUserFollowRepository.countPublicFollowPage(12L)).thenReturn(8L);

        PublicFollowUserItem item = new PublicFollowUserItem();
        item.setUserId(21L);
        item.setUsername("fan-a");
        PublicFollowUserVO vo = new PublicFollowUserVO();
        vo.setUserId(21L);
        vo.setUsername("fan-a");
        when(sysUserFollowRepository.selectPublicFollowPage(12L, 5L, 5L)).thenReturn(List.of(item));
        when(followModelMapper.toPublicFollowUserVO(item)).thenReturn(vo);

        PageResult<PublicFollowUserVO> result = publicFollowService.pageUserFollows(12L, query);

        assertEquals(8L, result.getTotal());
        assertEquals(2L, result.getCurrent());
        assertEquals(5L, result.getSize());
        assertEquals(1, result.getRecords().size());
        assertEquals("fan-a", result.getRecords().get(0).getUsername());
    }

    @Test
    void pageUserFansShouldReturnEmptyPageWhenNoData() {
        when(sysUserRepository.getById(12L)).thenReturn(activeUser(12L));
        when(sysUserFollowRepository.countPublicFanPage(12L)).thenReturn(0L);

        PageResult<PublicFollowUserVO> result = publicFollowService.pageUserFans(12L, null);

        assertEquals(0L, result.getTotal());
        assertEquals(1L, result.getCurrent());
        assertEquals(10L, result.getSize());
        assertEquals(List.of(), result.getRecords());
        verify(sysUserFollowRepository, never()).selectPublicFanPage(12L, 0L, 10L);
    }

    private SysUser activeUser(Long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setStatus(1);
        user.setDeletedFlag(0);
        return user;
    }
}
