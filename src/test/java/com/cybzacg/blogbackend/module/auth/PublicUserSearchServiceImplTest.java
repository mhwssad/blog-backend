package com.cybzacg.blogbackend.module.auth;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import com.cybzacg.blogbackend.module.auth.account.convert.UserProfileModelConvert;
import com.cybzacg.blogbackend.module.auth.account.model.user.PublicUserSearchVO;
import com.cybzacg.blogbackend.module.auth.account.service.impl.PublicUserSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicUserSearchServiceImplTest {
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private UserProfileModelConvert userProfileModelConvert;

    private PublicUserSearchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PublicUserSearchServiceImpl(sysUserRepository, userProfileModelConvert);
    }

    @Test
    void searchShouldReturnPageResult() {
        SysUser u = new SysUser();
        u.setId(1L);
        u.setNickname("TestUser");
        Page<SysUser> page = new Page<>(1, 20, 1);
        page.setRecords(List.of(u));

        when(sysUserRepository.searchByKeyword(eq("test"), anyLong(), anyLong())).thenReturn(page);

        PublicUserSearchVO vo = new PublicUserSearchVO();
        vo.setId(1L);
        vo.setNickname("TestUser");
        when(userProfileModelConvert.toPublicUserSearchVO(u)).thenReturn(vo);

        PageResult<PublicUserSearchVO> result = service.searchUsers(" test ", 1, 20);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("TestUser", result.getRecords().get(0).getNickname());
    }

    @Test
    void searchShouldReturnEmptyWhenNoMatch() {
        Page<SysUser> empty = new Page<>(1, 20, 0);
        empty.setRecords(List.of());
        when(sysUserRepository.searchByKeyword(any(), anyLong(), anyLong())).thenReturn(empty);

        PageResult<PublicUserSearchVO> result = service.searchUsers("nonexistent", 1, 20);

        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }
}
