package com.cybzacg.blogbackend.module.content;

import com.cybzacg.blogbackend.domain.SysCategory;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.article.repository.BlogArticleCategoryRepository;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.admin.CategoryAdminVO;
import com.cybzacg.blogbackend.module.content.model.admin.CategorySaveRequest;
import com.cybzacg.blogbackend.module.content.repository.SysCategoryRepository;
import com.cybzacg.blogbackend.module.content.service.impl.CategoryAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryAdminServiceImplTest {
    @Mock
    private SysCategoryRepository sysCategoryRepository;
    @Mock
    private BlogArticleCategoryRepository blogArticleCategoryService;
    @Mock
    private ContentModelMapper contentModelMapper;

    private CategoryAdminServiceImpl categoryAdminService;

    @BeforeEach
    void setUp() {
        categoryAdminService = new CategoryAdminServiceImpl(sysCategoryRepository, blogArticleCategoryService, contentModelMapper);
    }

    @Test
    void getCategoryShouldReturnMappedCategory() {
        SysCategory category = category(10L, 0L, "backend", "article");
        CategoryAdminVO vo = new CategoryAdminVO();
        vo.setId(10L);
        vo.setCode("backend");

        when(sysCategoryRepository.getById(10L)).thenReturn(category);
        when(contentModelMapper.toCategoryAdminVO(category)).thenReturn(vo);

        CategoryAdminVO result = categoryAdminService.getCategory(10L);

        assertSame(vo, result);
    }

    @Test
    void createCategoryShouldComputeHierarchyFieldsAndDefaults() {
        CategorySaveRequest request = new CategorySaveRequest();
        request.setParentId(5L);
        request.setName("Java");
        request.setCode("java");
        request.setType("article");

        SysCategory parent = category(5L, 1L, "language", "article");
        parent.setLevel(2);
        parent.setAncestors("0,1");

        SysCategory category = new SysCategory();
        CategoryAdminVO vo = new CategoryAdminVO();
        vo.setId(20L);

        when(sysCategoryRepository.existsByTypeAndCodeExcludingId("article", "java", null)).thenReturn(false);
        when(sysCategoryRepository.getById(5L)).thenReturn(parent);
        when(contentModelMapper.toCategory(request)).thenReturn(category);
        doAnswer(invocation -> {
            CategorySaveRequest actualRequest = invocation.getArgument(0);
            SysCategory actualCategory = invocation.getArgument(1);
            actualCategory.setName(actualRequest.getName());
            actualCategory.setCode(actualRequest.getCode());
            actualCategory.setType(actualRequest.getType());
            return null;
        }).when(contentModelMapper).updateCategory(request, category);
        when(sysCategoryRepository.save(category)).thenAnswer(invocation -> {
            category.setId(20L);
            return true;
        });
        when(contentModelMapper.toCategoryAdminVO(category)).thenReturn(vo);

        CategoryAdminVO result = categoryAdminService.createCategory(request);

        assertSame(vo, result);
        assertEquals(Long.valueOf(20L), category.getId());
        assertEquals(Integer.valueOf(3), category.getLevel());
        assertEquals("0,1,5", category.getAncestors());
        assertEquals(Integer.valueOf(0), category.getSortOrder());
        assertEquals(Integer.valueOf(1), category.getStatus());
        verify(sysCategoryRepository).save(category);
    }

    @Test
    void updateCategoryShouldRefreshChildHierarchy() {
        CategorySaveRequest request = new CategorySaveRequest();
        request.setParentId(20L);
        request.setName("Java Updated");
        request.setCode("java-updated");
        request.setType("article");
        request.setStatus(2);
        request.setSortOrder(8);

        SysCategory category = category(10L, 0L, "java", "article");
        category.setLevel(1);
        category.setAncestors("0");

        SysCategory parent = category(20L, 1L, "backend", "article");
        parent.setLevel(2);
        parent.setAncestors("0,1");

        SysCategory child = category(11L, 10L, "spring", "article");
        child.setLevel(2);
        child.setAncestors("0,10");

        CategoryAdminVO vo = new CategoryAdminVO();
        vo.setId(10L);

        when(sysCategoryRepository.getById(10L)).thenReturn(category);
        when(sysCategoryRepository.getById(20L)).thenReturn(parent);
        when(sysCategoryRepository.existsByTypeAndCodeExcludingId("article", "java-updated", 10L)).thenReturn(false);
        when(sysCategoryRepository.findByParentId(10L)).thenReturn(java.util.List.of(child));
        when(sysCategoryRepository.findByParentId(11L)).thenReturn(java.util.List.of());
        doAnswer(invocation -> {
            CategorySaveRequest actualRequest = invocation.getArgument(0);
            SysCategory actualCategory = invocation.getArgument(1);
            actualCategory.setName(actualRequest.getName());
            actualCategory.setCode(actualRequest.getCode());
            actualCategory.setType(actualRequest.getType());
            actualCategory.setStatus(actualRequest.getStatus());
            actualCategory.setSortOrder(actualRequest.getSortOrder());
            return null;
        }).when(contentModelMapper).updateCategory(request, category);
        when(contentModelMapper.toCategoryAdminVO(category)).thenReturn(vo);

        CategoryAdminVO result = categoryAdminService.updateCategory(10L, request);

        assertSame(vo, result);
        assertEquals(Integer.valueOf(3), category.getLevel());
        assertEquals("0,1,20", category.getAncestors());
        assertEquals(Integer.valueOf(4), child.getLevel());
        assertEquals("0,1,20,10", child.getAncestors());
        verify(sysCategoryRepository).updateById(category);
        verify(sysCategoryRepository).updateById(child);
    }

    @Test
    void updateStatusShouldUpdateCategoryStatus() {
        SysCategory category = category(10L, 0L, "java", "article");
        category.setStatus(1);
        when(sysCategoryRepository.getById(10L)).thenReturn(category);

        categoryAdminService.updateStatus(10L, 2);

        assertEquals(Integer.valueOf(2), category.getStatus());
        verify(sysCategoryRepository).updateById(category);
    }

    @Test
    void deleteCategoryShouldRemoveWhenNoChildrenAndNoBinding() {
        SysCategory category = category(10L, 0L, "java", "article");
        when(sysCategoryRepository.getById(10L)).thenReturn(category);
        when(sysCategoryRepository.existsByParentId(10L)).thenReturn(false);
        when(blogArticleCategoryService.existsByCategoryId(10L)).thenReturn(false);

        categoryAdminService.deleteCategory(10L);

        verify(sysCategoryRepository).removeById(10L);
    }

    @Test
    void deleteCategoryShouldThrowWhenChildrenExist() {
        SysCategory category = category(10L, 0L, "java", "article");
        when(sysCategoryRepository.getById(10L)).thenReturn(category);
        when(sysCategoryRepository.existsByParentId(10L)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> categoryAdminService.deleteCategory(10L));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("当前分类存在子分类，无法删除", exception.getMessage());
        verify(blogArticleCategoryService, never()).existsByCategoryId(any());
        verify(sysCategoryRepository, never()).removeById(10L);
    }

    @Test
    void deleteCategoryShouldThrowWhenArticleBindingExists() {
        SysCategory category = category(10L, 0L, "java", "article");
        when(sysCategoryRepository.getById(10L)).thenReturn(category);
        when(sysCategoryRepository.existsByParentId(10L)).thenReturn(false);
        when(blogArticleCategoryService.existsByCategoryId(10L)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> categoryAdminService.deleteCategory(10L));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("当前分类已绑定文章，无法删除", exception.getMessage());
        verify(sysCategoryRepository, never()).removeById(10L);
    }

    private SysCategory category(Long id, Long parentId, String code, String type) {
        SysCategory category = new SysCategory();
        category.setId(id);
        category.setParentId(parentId);
        category.setCode(code);
        category.setType(type);
        return category;
    }
}
