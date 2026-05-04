package com.cybzacg.blogbackend.module.ai.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cybzacg.blogbackend.domain.ai.AiAgentTaskLog;

import java.util.List;

/**
 * AiAgentTaskLog Repository。
 */
public interface AiAgentTaskLogRepository extends IService<AiAgentTaskLog> {

    /**
     * 按任务 ID 查询日志列表，按轮次序号排序。
     */
    List<AiAgentTaskLog> listByTaskId(Long taskId);
}
