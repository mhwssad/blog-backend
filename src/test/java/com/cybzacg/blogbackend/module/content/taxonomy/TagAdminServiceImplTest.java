package com.cybzacg.blogbackend.module.content.taxonomy;

import com.cybzacg.blogbackend.dto.domain.content.SysTag;
import com.cybzacg.blogbackend.module.content.shared.convert.ContentModelConvert;
import com.cybzacg.blogbackend.module.content.taxonomy.model.admin.TagSaveRequest;
import com.cybzacg.blogbackend.module.content.taxonomy.model.admin.TagVO;
import com.cybzacg.blogbackend.dto.repository.content.SysTagRelationRepository;
import com.cybzacg.blogbackend.dto.repository.content.SysTagRepository;
import com.cybzacg.blogbackend.module.content.taxonomy.service.impl.TagAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagAdminServiceImplTest {
    @Mock
    private SysTagRepository sysTagRepository;
    @Mock
    private SysTagRelationRepository sysTagRelationRepository;
    @Mock
    private ContentModelConvert contentModelConvert;

    private TagAdminServiceImpl tagAdminService;

    @BeforeEach
    void setUp() {
        tagAdminService = new TagAdminServiceImpl(sysTagRepository, sysTagRelationRepository, contentModelConvert);
    }

    @Test
    void listTagsShouldReturnMappedTagsInQueryOrder() {
        SysTag first = tag(2L, "Java", "#f00");
        SysTag second = tag(1L, "Spring", "#0f0");
        TagVO firstVo = tagVO(2L, "Java", "#f00");
        TagVO secondVo = tagVO(1L, "Spring", "#0f0");

        when(sysTagRepository.findAllOrderByIdDesc()).thenReturn(List.of(first, second));
        when(contentModelConvert.toTagVO(first)).thenReturn(firstVo);
        when(contentModelConvert.toTagVO(second)).thenReturn(secondVo);

        List<TagVO> result = tagAdminService.listTags();

        assertEquals(List.of(firstVo, secondVo), result);
        verify(sysTagRepository).findAllOrderByIdDesc();
    }

    @Test
    void getTagShouldReturnMappedTag() {
        SysTag tag = tag(1L, "Java", "#f00");
        TagVO vo = tagVO(1L, "Java", "#f00");
        when(sysTagRepository.getById(1L)).thenReturn(tag);
        when(contentModelConvert.toTagVO(tag)).thenReturn(vo);

        TagVO result = tagAdminService.getTag(1L);

        assertSame(vo, result);
    }

    @Test
    void createTagShouldSaveMappedTagWhenNameUnique() {
        TagSaveRequest request = new TagSaveRequest();
        request.setName(" Java ");
        request.setColor("#f00");

        SysTag tag = tag(null, "Java", "#f00");
        TagVO vo = tagVO(10L, "Java", "#f00");

        when(sysTagRepository.findByName("Java")).thenReturn(null);
        when(contentModelConvert.toTag(request)).thenReturn(tag);
        when(sysTagRepository.save(tag)).thenAnswer(invocation -> {
            tag.setId(10L);
            return true;
        });
        when(contentModelConvert.toTagVO(tag)).thenReturn(vo);

        TagVO result = tagAdminService.createTag(request);

        assertSame(vo, result);
        assertEquals(10L, tag.getId());
        verify(sysTagRepository).save(tag);
    }

    @Test
    void createTagShouldReturnExistingTagWhenNameAlreadyExists() {
        TagSaveRequest request = new TagSaveRequest();
        request.setName("Java");

        SysTag existing = tag(10L, "Java", "#000");
        TagVO existingVo = tagVO(10L, "Java", "#000");

        when(sysTagRepository.findByName("Java")).thenReturn(existing);
        when(contentModelConvert.toTagVO(existing)).thenReturn(existingVo);

        TagVO result = tagAdminService.createTag(request);

        assertSame(existingVo, result);
        verify(sysTagRepository, never()).save(any(SysTag.class));
    }

    @Test
    void updateTagShouldUpdateExistingTagWhenNameUnique() {
        SysTag existing = tag(3L, "Old", "#000");
        TagSaveRequest request = new TagSaveRequest();
        request.setName(" New ");
        request.setColor("#123");
        TagVO vo = tagVO(3L, "New", "#123");

        when(sysTagRepository.getById(3L)).thenReturn(existing);
        when(sysTagRepository.existsByNameExcludingId("New", 3L)).thenReturn(false);
        doAnswer(invocation -> {
            TagSaveRequest actualRequest = invocation.getArgument(0);
            SysTag actualTag = invocation.getArgument(1);
            actualTag.setName(actualRequest.getName().trim());
            actualTag.setColor(actualRequest.getColor());
            return null;
        }).when(contentModelConvert).updateTag(request, existing);
        when(contentModelConvert.toTagVO(existing)).thenReturn(vo);

        TagVO result = tagAdminService.updateTag(3L, request);

        assertSame(vo, result);
        assertEquals("New", existing.getName());
        assertEquals("#123", existing.getColor());
        verify(sysTagRepository).updateById(existing);
    }

    @Test
    void deleteTagShouldCleanupRelationsBeforeRemovingTag() {
        SysTag tag = tag(5L, "Java", "#f00");
        when(sysTagRepository.getById(5L)).thenReturn(tag);
        when(sysTagRelationRepository.removeByTagId(5L)).thenReturn(true);

        tagAdminService.deleteTag(5L);

        verify(sysTagRelationRepository).removeByTagId(5L);
        verify(sysTagRepository).removeById(5L);
    }

    private SysTag tag(Long id, String name, String color) {
        SysTag tag = new SysTag();
        tag.setId(id);
        tag.setName(name);
        tag.setColor(color);
        return tag;
    }

    private TagVO tagVO(Long id, String name, String color) {
        TagVO vo = new TagVO();
        vo.setId(id);
        vo.setName(name);
        vo.setColor(color);
        return vo;
    }
}
