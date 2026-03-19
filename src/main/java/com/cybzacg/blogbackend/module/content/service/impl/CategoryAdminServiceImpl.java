package com.cybzacg.blogbackend.module.content.service.impl;

import com.cybzacg.blogbackend.domain.BlogArticleCategory;
import com.cybzacg.blogbackend.domain.SysCategory;
import com.cybzacg.blogbackend.enums.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.admin.CategoryAdminVO;
import com.cybzacg.blogbackend.module.content.model.admin.CategorySaveRequest;
import com.cybzacg.blogbackend.module.content.model.admin.CategoryTreeVO;
import com.cybzacg.blogbackend.module.content.service.CategoryAdminService;
import com.cybzacg.blogbackend.module.content.service.SysCategoryService;
import com.cybzacg.blogbackend.module.article.service.BlogArticleCategoryService;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    private final SysCategoryService sysCategoryService;
    private final BlogArticleCategoryService blogArticleCategoryService;
    private final ContentModelMapper contentModelMapper;

    @Override
    public List<CategoryTreeVO> listCategoryTree() {
        List<SysCategory> categories = sysCategoryService.lambdaQuery()
                .eq(SysCategory::getType, ARTICLE_TYPE)
                .orderByAsc(SysCategory::getSortOrder)
                .orderByAsc(SysCategory::getId)
                .list();
        return buildCategoryTree(categories);
    }

    @Override
    public CategoryAdminVO getCategory(Long id) {
        return contentModelMapper.toCategoryAdminVO(getCategoryOrThrow(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryAdminVO createCategory(CategorySaveRequest request) {
        validateRequest(request, null);
        SysCategory parent = validateParent(request.getParentId(), null);
        SysCategory category = new SysCategory();
        applyFields(category, request, parent);
        sysCategoryService.save(category);
        return contentModelMapper.toCategoryAdminVO(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryAdminVO updateCategory(Long id, CategorySaveRequest request) {
        SysCategory category = getCategoryOrThrow(id);
        validateRequest(request, id);
        SysCategory parent = validateParent(request.getParentId(), id);
        applyFields(category, request, parent);
        sysCategoryService.updateById(category);
        refreshChildrenHierarchy(category);
        return contentModelMapper.toCategoryAdminVO(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        SysCategory category = getCategoryOrThrow(id);
        category.setStatus(status);
        sysCategoryService.updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        getCategoryOrThrow(id);
        boolean hasChildren = sysCategoryService.lambdaQuery().eq(SysCategory::getParentId, id).exists();
        if (hasChildren) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "当前分类存在子分类，无法删除");
        }
        boolean boundArticle = blogArticleCategoryService.lambdaQuery().eq(BlogArticleCategory::getCategoryId, id).exists();
        if (boundArticle) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "当前分类已绑定文章，无法删除");
        }
        sysCategoryService.removeById(id);
    }

    /**
     * 校验分类请求是否合法，目前仅允许维护文章分类，且分类编码需唯一。
     */
    private void validateRequest(CategorySaveRequest request, Long currentId) {
        if (!StringUtils.hasText(request.getType()) || !ARTICLE_TYPE.equals(StrUtils.trim(request.getType()))) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "当前仅支持文章分类");
        }
        boolean duplicated = sysCategoryService.lambdaQuery()
                .eq(SysCategory::getType, StrUtils.trim(request.getType()))
                .eq(SysCategory::getCode, StrUtils.trim(request.getCode()))
                .ne(currentId != null, SysCategory::getId, currentId)
                .exists();
        if (duplicated) {
            throw new BusinessException(ResultErrorCode.DATA_ALREADY_EXISTS.getCode(), "分类编码已存在");
        }
    }

    /**
     * 校验父分类是否合法，避免自关联和挂载到自身子孙节点。
     */
    private SysCategory validateParent(Long parentId, Long currentId) {
        if (parentId == null || ROOT_PARENT_ID == parentId) {
            return null;
        }
        if (currentId != null && currentId.equals(parentId)) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "父分类不能为自身");
        }
        SysCategory parent = getCategoryOrThrow(parentId);
        if (currentId != null && isDescendant(parent, currentId)) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "父分类不能选择当前分类的子节点");
        }
        return parent;
    }

    /**
     * 判断候选父分类是否属于当前分类的后代，防止分类树形成环。
     */
    private boolean isDescendant(SysCategory parent, Long currentId) {
        if (!StringUtils.hasText(parent.getAncestors())) {
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
        category.setParentId(request.getParentId());
        category.setName(StrUtils.trim(request.getName()));
        category.setCode(StrUtils.trim(request.getCode()));
        category.setType(StrUtils.trim(request.getType()));
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        category.setIcon(StrUtils.normalize(request.getIcon()));
        category.setDescription(StrUtils.normalize(request.getDescription()));
        category.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        category.setLevel(parent == null ? 1 : parent.getLevel() + 1);
        category.setAncestors(parent == null ? "0" : buildAncestors(parent));
    }

    private String buildAncestors(SysCategory parent) {
        if (!StringUtils.hasText(parent.getAncestors())) {
            return String.valueOf(parent.getId());
        }
        return parent.getAncestors() + "," + parent.getId();
    }

    /**
     * 递归刷新子分类层级和祖先链，保证移动分类后整棵子树结构一致。
     */
    private void refreshChildrenHierarchy(SysCategory category) {
        List<SysCategory> children = sysCategoryService.lambdaQuery()
                .eq(SysCategory::getParentId, category.getId())
                .list();
        for (SysCategory child : children) {
            child.setLevel(category.getLevel() + 1);
            child.setAncestors(buildAncestors(category));
            sysCategoryService.updateById(child);
            refreshChildrenHierarchy(child);
        }
    }

    /**
     * 将分类列表组装成树结构，供后台管理页直接展示。
     */
    private List<CategoryTreeVO> buildCategoryTree(List<SysCategory> categories) {
        Map<Long, CategoryTreeVO> categoryMap = new LinkedHashMap<>();
        for (SysCategory category : categories) {
            categoryMap.put(category.getId(), contentModelMapper.toCategoryTreeVO(category));
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
        SysCategory category = sysCategoryService.getById(id);
        if (category == null) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "分类不存在");
        }
        return category;
    }

}

