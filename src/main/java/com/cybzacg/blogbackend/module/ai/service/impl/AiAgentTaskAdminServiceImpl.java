package com.cybzacg.blogbackend.module.ai.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.ai.AiAgentDefinition;
import com.cybzacg.blogbackend.domain.ai.AiAgentTask;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.ai.convert.AiModelConvert;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentTaskAdminPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentTaskAdminVO;
import com.cybzacg.blogbackend.module.ai.repository.AiAgentDefinitionRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiAgentTaskRepository;
import com.cybzacg.blogbackend.module.ai.service.AiAgentTaskAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI Agent 任务后台管理服务实现。
 */
@Service
@RequiredArgsConstructor
public class AiAgentTaskAdminServiceImpl implements AiAgentTaskAdminService {

    private final AiAgentTaskRepository aiAgentTaskRepository;
    private final AiAgentDefinitionRepository aiAgentDefinitionRepository;
    private final AiModelConvert aiModelConvert;

    @Override
    public PageResult<AiAgentTaskAdminVO> pageTasks(AiAgentTaskAdminPageQuery query) {
        long current = PaginationUtils.normalizeCurrent(query.getCurrent());
        long size = PaginationUtils.normalizeSize(query.getSize(), 20L, 100L);
        Page<AiAgentTask> page = new Page<>(current, size);
        Page<AiAgentTask> result = aiAgentTaskRepository.pageByAgentIdAndStatus(
                page, query.getAgentId(), query.getStatus());

        List<AiAgentTaskAdminVO> voList = result.getRecords().stream().map(task -> {
            AiAgentTaskAdminVO vo = aiModelConvert.toAgentTaskAdminVO(task);
            AiAgentDefinition def = aiAgentDefinitionRepository.getById(task.getAgentId());
            if (def != null) {
                vo.setAgentName(def.getName());
            }
            return vo;
        }).toList();

        return PageResult.of(result, voList);
    }

    @Override
    public AiAgentTaskAdminVO getTask(Long id) {
        AiAgentTask task = ExceptionThrowerCore.requireNonNull(
                aiAgentTaskRepository.getById(id),
                ResultErrorCode.AI_AGENT_TASK_NOT_FOUND);
        AiAgentTaskAdminVO vo = aiModelConvert.toAgentTaskAdminVO(task);
        AiAgentDefinition def = aiAgentDefinitionRepository.getById(task.getAgentId());
        if (def != null) {
            vo.setAgentName(def.getName());
        }
        return vo;
    }
}
