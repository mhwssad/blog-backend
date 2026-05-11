package com.cybzacg.blogbackend.module.forum;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.forum.ForumSection;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.forum.ForumVisibilityScopeEnum;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.forum.convert.ForumModelConvert;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumSectionAdminVO;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumSectionPageQuery;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumSectionSaveRequest;
import com.cybzacg.blogbackend.dto.repository.forum.ForumPostRepository;
import com.cybzacg.blogbackend.dto.repository.forum.ForumSectionRepository;
import com.cybzacg.blogbackend.module.forum.service.impl.ForumSectionAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForumSectionAdminServiceImplTest {
    @Mock
    private ForumSectionRepository forumSectionRepository;
    @Mock
    private ForumPostRepository forumPostRepository;
    @Mock
    private ForumModelConvert forumModelConvert;

    private ForumSectionAdminServiceImpl forumSectionAdminService;

    @BeforeEach
    void setUp() {
        forumSectionAdminService = new ForumSectionAdminServiceImpl(
                forumSectionRepository,
                forumPostRepository,
                forumModelConvert
        );
    }

    @Test
    void pageSectionsShouldNormalizeQueryAndConvertRecords() {
        ForumSectionPageQuery query = new ForumSectionPageQuery();
        query.setCurrent(0L);
        query.setSize(200L);
        query.setKeyword("  java  ");
        query.setStatus(1);
        query.setVisibilityScope(ForumVisibilityScopeEnum.PUBLIC.getValue());
        ForumSection section = section(10L, "Java");
        Page<ForumSection> page = new Page<>(1, 100);
        page.setTotal(1);
        page.setRecords(List.of(section));
        ForumSectionAdminVO vo = vo(10L, "Java");
        when(forumSectionRepository.pageAdminSections(query)).thenReturn(page);
        when(forumModelConvert.toSectionAdminVO(section)).thenReturn(vo);

        PageResult<ForumSectionAdminVO> result = forumSectionAdminService.pageSections(query);

        assertEquals(1L, result.getCurrent());
        assertEquals(100L, result.getSize());
        assertEquals(1L, result.getTotal());
        assertEquals("Java", result.getRecords().get(0).getName());
        assertEquals(1L, query.getCurrent());
        assertEquals(100L, query.getSize());
        assertEquals("java", query.getKeyword());
    }

    @Test
    void pageSectionsShouldRejectInvalidStatus() {
        ForumSectionPageQuery query = new ForumSectionPageQuery();
        query.setStatus(2);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> forumSectionAdminService.pageSections(query));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        verify(forumSectionRepository, never()).pageAdminSections(any());
    }

    @Test
    void getSectionShouldReturnConvertedSection() {
        ForumSection section = section(10L, "Java");
        ForumSectionAdminVO vo = vo(10L, "Java");
        when(forumSectionRepository.getById(10L)).thenReturn(section);
        when(forumModelConvert.toSectionAdminVO(section)).thenReturn(vo);

        ForumSectionAdminVO result = forumSectionAdminService.getSection(10L);

        assertEquals(10L, result.getId());
        assertEquals("Java", result.getName());
    }

    @Test
    void getSectionShouldRejectMissingSection() {
        when(forumSectionRepository.getById(404L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> forumSectionAdminService.getSection(404L));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
    }

    @Test
    void createSectionShouldApplyDefaultsAndSave() {
        ForumSectionSaveRequest request = saveRequest(" Java ");
        request.setDescription("  编程讨论  ");
        ForumSection section = new ForumSection();
        section.setName("Java");
        section.setDescription("编程讨论");
        ForumSectionAdminVO vo = vo(10L, "Java");
        when(forumSectionRepository.existsByNameExcludingId("Java", null)).thenReturn(false);
        when(forumModelConvert.toSection(request)).thenReturn(section);
        when(forumModelConvert.toSectionAdminVO(section)).thenReturn(vo);

        ForumSectionAdminVO result = forumSectionAdminService.createSection(request);

        assertEquals(10L, result.getId());
        assertEquals(0, section.getSortOrder());
        assertEquals(ForumVisibilityScopeEnum.PUBLIC.getValue(), section.getVisibilityScope());
        assertEquals(1, section.getPostLevelLimit());
        assertEquals(1, section.getStatus());
        verify(forumSectionRepository).save(section);
    }

    @Test
    void createSectionShouldRejectDuplicatedName() {
        ForumSectionSaveRequest request = saveRequest(" Java ");
        when(forumSectionRepository.existsByNameExcludingId("Java", null)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> forumSectionAdminService.createSection(request));

        assertEquals(ResultErrorCode.DATA_ALREADY_EXISTS.getCode(), exception.getCode());
        verify(forumSectionRepository, never()).save(any(ForumSection.class));
    }

    @Test
    void updateSectionShouldValidateAndKeepExplicitValues() {
        ForumSection existing = section(10L, "Old");
        ForumSectionSaveRequest request = saveRequest(" New ");
        request.setSortOrder(3);
        request.setVisibilityScope(ForumVisibilityScopeEnum.LOGIN_ONLY.getValue());
        request.setPostLevelLimit(5);
        request.setStatus(0);
        ForumSectionAdminVO vo = vo(10L, "New");
        when(forumSectionRepository.getById(10L)).thenReturn(existing);
        when(forumSectionRepository.existsByNameExcludingId("New", 10L)).thenReturn(false);
        when(forumModelConvert.toSectionAdminVO(existing)).thenReturn(vo);

        forumSectionAdminService.updateSection(10L, request);

        verify(forumModelConvert).updateSection(request, existing);
        verify(forumSectionRepository).updateById(existing);
    }

    @Test
    void updateStatusShouldRejectInvalidStatus() {
        ForumSection section = section(10L, "Java");
        when(forumSectionRepository.getById(10L)).thenReturn(section);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> forumSectionAdminService.updateStatus(10L, 2));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        verify(forumSectionRepository, never()).updateById(any(ForumSection.class));
    }

    @Test
    void updateStatusShouldPersistValidStatus() {
        ForumSection section = section(10L, "Java");
        when(forumSectionRepository.getById(10L)).thenReturn(section);

        forumSectionAdminService.updateStatus(10L, 0);

        ArgumentCaptor<ForumSection> captor = ArgumentCaptor.forClass(ForumSection.class);
        verify(forumSectionRepository).updateById(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
    }

    @Test
    void deleteSectionShouldRemoveEmptySection() {
        when(forumSectionRepository.getById(10L)).thenReturn(section(10L, "Java"));
        when(forumPostRepository.existsBySectionId(10L)).thenReturn(false);

        forumSectionAdminService.deleteSection(10L);

        verify(forumSectionRepository).removeById(10L);
    }

    @Test
    void deleteSectionShouldRejectSectionWithPosts() {
        when(forumSectionRepository.getById(10L)).thenReturn(section(10L, "Java"));
        when(forumPostRepository.existsBySectionId(10L)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> forumSectionAdminService.deleteSection(10L));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        verify(forumSectionRepository, never()).removeById(eq(10L));
    }

    private ForumSectionSaveRequest saveRequest(String name) {
        ForumSectionSaveRequest request = new ForumSectionSaveRequest();
        request.setName(name);
        return request;
    }

    private ForumSection section(Long id, String name) {
        ForumSection section = new ForumSection();
        section.setId(id);
        section.setName(name);
        section.setStatus(1);
        return section;
    }

    private ForumSectionAdminVO vo(Long id, String name) {
        ForumSectionAdminVO vo = new ForumSectionAdminVO();
        vo.setId(id);
        vo.setName(name);
        return vo;
    }
}
