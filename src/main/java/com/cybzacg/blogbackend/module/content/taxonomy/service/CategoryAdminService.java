package com.cybzacg.blogbackend.module.content.taxonomy.service;

import com.cybzacg.blogbackend.module.content.taxonomy.model.admin.CategoryAdminVO;
import com.cybzacg.blogbackend.module.content.taxonomy.model.admin.CategorySaveRequest;
import com.cybzacg.blogbackend.module.content.taxonomy.model.admin.CategoryTreeVO;

import java.util.List;

/**
 * 分类后台管理服务接口。
 *
 * <p>定义分类后台管理相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface CategoryAdminService {
    List<CategoryTreeVO> listCategoryTree();

    CategoryAdminVO getCategory(Long id);

    CategoryAdminVO createCategory(CategorySaveRequest request);

    CategoryAdminVO updateCategory(Long id, CategorySaveRequest request);

    void updateStatus(Long id, Integer status);

    void deleteCategory(Long id);
}
