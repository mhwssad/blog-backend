package com.cybzacg.blogbackend.module.content.interaction.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.content.interaction.model.admin.InteractionPageQuery;
import com.cybzacg.blogbackend.module.content.interaction.model.admin.InteractionVO;

/**
 * 互动后台管理服务接口。
 *
 * <p>定义互动后台管理相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface InteractionAdminService {
    PageResult<InteractionVO> pageInteractions(InteractionPageQuery query);

    void deleteInteraction(Long id);
}
