package com.cybzacg.blogbackend.module.content.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.content.model.user.UserFootprintPageQuery;
import com.cybzacg.blogbackend.module.content.model.user.UserFootprintVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户足迹服务接口。
 *
 * <p>定义用户足迹相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface UserFootprintService {
    PageResult<UserFootprintVO> pageFootprints(UserFootprintPageQuery query);

    void deleteFootprint(Long id);

    void clearFootprints();

    void recordArticleFootprint(Long articleId, HttpServletRequest request);
}
