package com.cybzacg.blogbackend.module.content.taxonomy.service;

import com.cybzacg.blogbackend.module.content.taxonomy.model.admin.TagSaveRequest;
import com.cybzacg.blogbackend.module.content.taxonomy.model.admin.TagVO;

import java.util.List;

/**
 * 标签后台管理服务接口。
 *
 * <p>定义标签后台管理相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface TagAdminService {
    List<TagVO> listTags();

    TagVO getTag(Long id);

    TagVO createTag(TagSaveRequest request);

    TagVO updateTag(Long id, TagSaveRequest request);

    void deleteTag(Long id);
}
