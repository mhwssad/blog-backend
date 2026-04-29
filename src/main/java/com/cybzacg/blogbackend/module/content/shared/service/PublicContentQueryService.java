package com.cybzacg.blogbackend.module.content.shared.service;

import com.cybzacg.blogbackend.module.content.taxonomy.model.publics.PublicCategoryTreeVO;
import com.cybzacg.blogbackend.module.content.comment.model.publics.PublicCommentQuery;
import com.cybzacg.blogbackend.module.content.comment.model.publics.PublicCommentVO;
import com.cybzacg.blogbackend.module.content.taxonomy.model.publics.PublicTagVO;

import java.util.List;

/**
 * 前台内容查询服务接口。
 *
 * <p>定义前台内容查询相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface PublicContentQueryService {
    List<PublicCategoryTreeVO> listCategoryTree();

    List<PublicTagVO> listTags(String targetType);

    List<PublicCommentVO> listComments(PublicCommentQuery query);
}
