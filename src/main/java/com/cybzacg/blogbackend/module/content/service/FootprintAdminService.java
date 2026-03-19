package com.cybzacg.blogbackend.module.content.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.content.model.admin.FootprintPageQuery;
import com.cybzacg.blogbackend.module.content.model.admin.FootprintVO;

/**
 * 足迹后台管理服务接口。
 *
 * <p>定义足迹后台管理相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface FootprintAdminService {
    PageResult<FootprintVO> pageFootprints(FootprintPageQuery query);

    void deleteFootprint(Long id);

    void cleanFootprints(FootprintPageQuery query);
}
