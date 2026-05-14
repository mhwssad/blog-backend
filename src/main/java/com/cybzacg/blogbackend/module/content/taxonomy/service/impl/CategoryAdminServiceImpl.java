package com.cybzacg.blogbackend.module.content.taxonomy.service.impl;

import com.cybzacg.blogbackend.dto.domain.content.SysCategory;
import com.cybzacg.blogbackend.dto.repository.article.BlogArticleCategoryRepository;
import com.cybzacg.blogbackend.dto.repository.content.SysCategoryRepository;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.content.shared.convert.ContentModelConvert;
import com.cybzacg.blogbackend.module.content.taxonomy.model.admin.CategoryAdminVO;
import com.cybzacg.blogbackend.module.content.taxonomy.model.admin.CategorySaveRequest;
import com.cybzacg.blogbackend.module.content.taxonomy.model.admin.CategoryTreeVO;
import com.cybzacg.blogbackend.module.content.taxonomy.service.CategoryAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 分类后台管理服务实现。
 *
 * <p>负责文章分类树查询、分类维护、层级关系刷新以及删除前引用校验。
 */
@Service
@RequiredArgsConstructor
public class CategoryAdminServiceImpl implements CategoryAdminService {
    private static final long ROOT_PARENT_ID = 0L;
    private static final String ARTICLE_TYPE = "article";

    private final SysCategoryRepository sysCategoryRepository;
    private final BlogArticleCategoryRepository blogArticleCategoryService;
    private final ContentModelConvert contentModelConvert;

    /**
     * 查询文章分类树结构，返回按排序字段组装的层级列表。
     */
    @Override
    public List<CategoryTreeVO> listCategoryTree() {
        List<SysCategory> categories = sysCategoryRepository.findByTypeOrderBySortOrderAndId(ARTICLE_TYPE);
        return buildCategoryTree(categories);
    }

    /**
     * 按ID获取分类详情。
     */
    @Override
    public CategoryAdminVO getCategory(Long id) {
        return contentModelConvert.toCategoryAdminVO(getCategoryOrThrow(id));
    }

    /**
     * 创建分类，校验编码唯一性与父分类合法性后持久化。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryAdminVO createCategory(CategorySaveRequest request) {
        validateRequest(request, null);
        SysCategory parent = validateParent(request.getParentId(), null);
        SysCategory category = contentModelConvert.toCategory(request);
        applyFields(category, request, parent);
        sysCategoryRepository.save(category);
        return contentModelConvert.toCategoryAdminVO(category);
    }

    /**
     * 更新分类信息，并递归刷新子分类的层级与祖先链。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryAdminVO updateCategory(Long id, CategorySaveRequest request) {
        SysCategory category = getCategoryOrThrow(id);
        validateRequest(request, id);
        SysCategory parent = validateParent(request.getParentId(), id);
        applyFields(category, request, parent);
        sysCategoryRepository.updateById(category);
        refreshChildrenHierarchy(category);
        return contentModelConvert.toCategoryAdminVO(category);
    }

    /**
     * 切换分类启用/禁用状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        SysCategory category = getCategoryOrThrow(id);
        category.setStatus(status);
        sysCategoryRepository.updateById(category);
    }

    /**
     * 删除分类，删除前校验是否仍存在子分类或已绑定文章。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        getCategoryOrThrow(id);
        boolean hasChildren = sysCategoryRepository.existsByParentId(id);
        ExceptionThrowerCore.throwBusinessIf(hasChildren, ResultErrorCode.ILLEGAL_ARGUMENT, "当前分类存在子分类，无法删除");
        boolean boundArticle = blogArticleCategoryService.existsByCategoryId(id);
        ExceptionThrowerCore.throwBusinessIf(boundArticle, ResultErrorCode.ILLEGAL_ARGUMENT, "当前分类已绑定文章，无法删除");
        sysCategoryRepository.removeById(id);
    }

    /**
     * 校验分类请求是否合法，目前仅允许维护文章分类，且分类编码需唯一。
     */
    private void validateRequest(CategorySaveRequest request, Long currentId) {
        boolean duplicated = sysCategoryRepository.existsByTypeAndCodeExcludingId(
                StrUtils.trim(request.getType()),
                StrUtils.trim(request.getCode()),
                currentId);
        ExceptionThrowerCore.throwBusinessIf(duplicated, ResultErrorCode.DATA_ALREADY_EXISTS, "分类编码已存在");
    }

    /**
     * 校验父分类是否合法，避免自关联和挂载到自身子孙节点。
     */
    private SysCategory validateParent(Long parentId, Long currentId) {
        if (parentId == null || ROOT_PARENT_ID == parentId) {
            return null;
        }
        ExceptionThrowerCore.throwBusinessIf(currentId != null && currentId.equals(parentId), ResultErrorCode.ILLEGAL_ARGUMENT, "父分类不能为自身");
        SysCategory parent = getCategoryOrThrow(parentId);
        ExceptionThrowerCore.throwBusinessIf(currentId != null && isDescendant(parent, currentId), ResultErrorCode.ILLEGAL_ARGUMENT, "父分类不能选择当前分类的子节点");
        return parent;
    }

    /**
     * 判断候选父分类是否属于当前分类的后代，防止分类树形成环。
     */
    private boolean isDescendant(SysCategory parent, Long currentId) {
        if (!StrUtils.hasText(parent.getAncestors())) {
            return false;
        }
        String[] segments = parent.getAncestors().split(",");
        for (String segment : segments) {
            if (String.valueOf(currentId).equals(StrUtils.trim(segment))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将请求字段和层级信息回填到分类实体，统一维护层级与祖先链。
     */
    private void applyFields(SysCategory category, CategorySaveRequest request, SysCategory parent) {
        contentModelConvert.updateCategory(request, category);
        category.setSortOrder(category.getSortOrder() == null ? 0 : category.getSortOrder());
        category.setStatus(category.getStatus() == null ? 1 : category.getStatus());
        category.setLevel(parent == null ? 1 : parent.getLevel() + 1);
        category.setAncestors(parent == null ? "0" : buildAncestors(parent));
    }

    private String buildAncestors(SysCategory parent) {
        if (!StrUtils.hasText(parent.getAncestors())) {
            return String.valueOf(parent.getId());
        }
        return parent.getAncestors() + "," + parent.getId();
    }

    /**
     * 递归刷新子分类层级和祖先链，保证移动分类后整棵子树结构一致。
     */
    private void refreshChildrenHierarchy(SysCategory category) {
        List<SysCategory> children = sysCategoryRepository.findByParentId(category.getId());
        for (SysCategory child : children) {
            child.setLevel(category.getLevel() + 1);
            child.setAncestors(buildAncestors(category));
            sysCategoryRepository.updateById(child);
            refreshChildrenHierarchy(child);
        }
    }

    /**
     * 将分类列表组装成树结构，供后台管理页直接展示。
     */
    private List<CategoryTreeVO> buildCategoryTree(List<SysCategory> categories) {
        Map<Long, CategoryTreeVO> categoryMap = new LinkedHashMap<>();
        for (SysCategory category : categories) {
            categoryMap.put(category.getId(), contentModelConvert.toCategoryTreeVO(category));
        }
        List<CategoryTreeVO> roots = new ArrayList<>();
        for (CategoryTreeVO node : categoryMap.values()) {
            CategoryTreeVO parent = categoryMap.get(node.getParentId());
            if (parent == null || Long.valueOf(ROOT_PARENT_ID).equals(node.getParentId())) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    /**
     * 按 ID 获取分类，不存在时抛出统一业务异常。
     */
    private SysCategory getCategoryOrThrow(Long id) {
        SysCategory category = sysCategoryRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(category, ResultErrorCode.ILLEGAL_ARGUMENT, "分类不存在");
        return category;
    }
}
