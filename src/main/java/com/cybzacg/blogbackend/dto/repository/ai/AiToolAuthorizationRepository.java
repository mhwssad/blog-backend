package com.cybzacg.blogbackend.module.ai.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.dto.domain.ai.AiToolAuthorization;

import java.util.List;

/**
 * AI 工具授权 Repository。
 */
public interface AiToolAuthorizationRepository extends IService<AiToolAuthorization> {
    List<AiToolAuthorization> listEnabledByToolId(Long toolId);
}
