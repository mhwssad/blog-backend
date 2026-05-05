package com.cybzacg.blogbackend.module.content.friendlink;

import com.cybzacg.blogbackend.domain.content.BlogFriendLink;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.content.friendlink.convert.FriendLinkModelConvert;
import com.cybzacg.blogbackend.module.content.friendlink.model.admin.FriendLinkSaveRequest;
import com.cybzacg.blogbackend.module.content.friendlink.model.admin.FriendLinkVO;
import com.cybzacg.blogbackend.module.content.friendlink.repository.BlogFriendLinkRepository;
import com.cybzacg.blogbackend.module.content.friendlink.service.impl.FriendLinkAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendLinkAdminServiceImplTest {
    @Mock
    private BlogFriendLinkRepository friendLinkRepository;
    @Mock
    private FriendLinkModelConvert friendLinkModelConvert;

    private FriendLinkAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FriendLinkAdminServiceImpl(friendLinkRepository, friendLinkModelConvert);
    }

    @Test
    void createShouldSaveLinkWithDefaultSortAndEnabledStatus() {
        FriendLinkSaveRequest request = new FriendLinkSaveRequest();
        request.setName("Test Blog");
        request.setUrl("https://example.com");

        BlogFriendLink converted = new BlogFriendLink();
        converted.setName("Test Blog");
        converted.setUrl("https://example.com");
        when(friendLinkModelConvert.toEntity(request)).thenReturn(converted);
        when(friendLinkRepository.save(any(BlogFriendLink.class))).thenAnswer(inv -> {
            BlogFriendLink link = inv.getArgument(0);
            link.setId(1L);
            return true;
        });
        when(friendLinkModelConvert.toVO(any(BlogFriendLink.class))).thenAnswer(inv -> {
            BlogFriendLink link = inv.getArgument(0);
            FriendLinkVO vo = new FriendLinkVO();
            vo.setId(link.getId());
            vo.setName(link.getName());
            vo.setSortOrder(link.getSortOrder());
            vo.setStatus(link.getStatus());
            return vo;
        });

        FriendLinkVO result = service.create(request);

        ArgumentCaptor<BlogFriendLink> captor = ArgumentCaptor.forClass(BlogFriendLink.class);
        verify(friendLinkRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getSortOrder());
        assertEquals(1, captor.getValue().getStatus());
        assertNotNull(result);
    }

    @Test
    void createShouldRespectProvidedSortOrder() {
        FriendLinkSaveRequest request = new FriendLinkSaveRequest();
        request.setName("Test");
        request.setUrl("https://example.com");
        request.setSortOrder(5);

        BlogFriendLink converted = new BlogFriendLink();
        converted.setSortOrder(5);
        when(friendLinkModelConvert.toEntity(request)).thenReturn(converted);
        when(friendLinkRepository.save(any())).thenReturn(true);

        service.create(request);

        ArgumentCaptor<BlogFriendLink> captor = ArgumentCaptor.forClass(BlogFriendLink.class);
        verify(friendLinkRepository).save(captor.capture());
        assertEquals(5, captor.getValue().getSortOrder());
    }

    @Test
    void updateShouldModifyExistingLink() {
        BlogFriendLink existing = link(1L, "Old Name", "https://old.com");
        when(friendLinkRepository.getById(1L)).thenReturn(existing);
        when(friendLinkRepository.updateById(any())).thenReturn(true);
        when(friendLinkModelConvert.toVO(any())).thenReturn(new FriendLinkVO());

        FriendLinkSaveRequest request = new FriendLinkSaveRequest();
        request.setName("New Name");
        request.setUrl("https://new.com");
        service.update(1L, request);

        verify(friendLinkModelConvert).updateEntity(eq(request), eq(existing));
        verify(friendLinkRepository).updateById(existing);
    }

    @Test
    void updateStatusShouldPersistNewStatus() {
        BlogFriendLink link = link(1L, "Test", "https://example.com");
        when(friendLinkRepository.getById(1L)).thenReturn(link);

        service.updateStatus(1L, 0);

        assertEquals(0, link.getStatus());
        verify(friendLinkRepository).updateById(link);
    }

    @Test
    void deleteShouldRemoveExistingLink() {
        when(friendLinkRepository.getById(1L)).thenReturn(link(1L, "Test", "https://example.com"));
        when(friendLinkRepository.removeById(1L)).thenReturn(true);

        service.delete(1L);

        verify(friendLinkRepository).removeById(1L);
    }

    @Test
    void getByIdShouldThrowWhenNotFound() {
        when(friendLinkRepository.getById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getById(999L));
        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), ex.getCode());
    }

    @Test
    void updateShouldThrowWhenNotFound() {
        when(friendLinkRepository.getById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.update(999L, new FriendLinkSaveRequest()));
        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), ex.getCode());
    }

    @Test
    void deleteShouldThrowWhenNotFound() {
        when(friendLinkRepository.getById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(999L));
        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), ex.getCode());
    }

    private BlogFriendLink link(Long id, String name, String url) {
        BlogFriendLink link = new BlogFriendLink();
        link.setId(id);
        link.setName(name);
        link.setUrl(url);
        link.setSortOrder(0);
        link.setStatus(1);
        return link;
    }
}
