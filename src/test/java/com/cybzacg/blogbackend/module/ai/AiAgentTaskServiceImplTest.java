package com.cybzacg.blogbackend.module.ai;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.ai.AiAgentDefinition;
import com.cybzacg.blogbackend.dto.domain.ai.AiAgentTask;
import com.cybzacg.blogbackend.dto.domain.ai.AiChannelConfig;
import com.cybzacg.blogbackend.enums.auth.NotificationTypeEnum;
import com.cybzacg.blogbackend.module.ai.convert.AiModelConvert;
import com.cybzacg.blogbackend.module.ai.model.data.AiModelCallResult;
import com.cybzacg.blogbackend.module.ai.model.user.AiAgentTaskCreateRequest;
import com.cybzacg.blogbackend.module.ai.model.user.AiAgentTaskPageQuery;
import com.cybzacg.blogbackend.module.ai.model.user.AiAgentTaskVO;
import com.cybzacg.blogbackend.dto.repository.ai.AiAgentDefinitionRepository;
import com.cybzacg.blogbackend.dto.repository.ai.AiAgentTaskLogRepository;
import com.cybzacg.blogbackend.dto.repository.ai.AiAgentTaskRepository;
import com.cybzacg.blogbackend.dto.repository.ai.AiChannelConfigRepository;
import com.cybzacg.blogbackend.module.ai.service.AiModelClient;
import com.cybzacg.blogbackend.module.ai.service.AiQuotaService;
import com.cybzacg.blogbackend.module.ai.service.AiToolExecutionService;
import com.cybzacg.blogbackend.module.ai.service.AiUsageLogService;
import com.cybzacg.blogbackend.module.ai.service.impl.AiAgentTaskServiceImpl;
import com.cybzacg.blogbackend.module.auth.notice.service.NotificationDeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AiAgentTaskServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class AiAgentTaskServiceImplTest {

    @Mock
    private AiAgentTaskRepository aiAgentTaskRepository;
    @Mock
    private AiAgentDefinitionRepository aiAgentDefinitionRepository;
    @Mock
    private AiAgentTaskLogRepository aiAgentTaskLogRepository;
    @Mock
    private AiChannelConfigRepository aiChannelConfigRepository;
    @Mock
    private AiModelClient aiModelClient;
    @Mock
    private AiQuotaService aiQuotaService;
    @Mock
    private AiToolExecutionService aiToolExecutionService;
    @Mock
    private AiUsageLogService aiUsageLogService;
    @Mock
    private NotificationDeliveryService notificationDeliveryService;
    @Mock
    private AiModelConvert aiModelConvert;

    private AiAgentTaskServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiAgentTaskServiceImpl(
                aiAgentTaskRepository,
                aiAgentDefinitionRepository,
                aiAgentTaskLogRepository,
                aiChannelConfigRepository,
                aiModelClient,
                aiQuotaService,
                aiToolExecutionService,
                aiUsageLogService,
                notificationDeliveryService,
                aiModelConvert
        );
    }

    @Test
    void pageMyTasksShouldNormalizePaginationAndFillAgentName() {
        Long userId = 1L;
        AiAgentTaskPageQuery query = new AiAgentTaskPageQuery();
        query.setCurrent(2L);
        query.setSize(500L);
        query.setStatus(2);

        AiAgentTask task = new AiAgentTask();
        task.setId(100L);
        task.setAgentId(10L);

        Page<AiAgentTask> page = new Page<>(2L, 100L);
        page.setTotal(1L);
        page.setRecords(List.of(task));
        when(aiAgentTaskRepository.pageByUserIdAndStatus(
                org.mockito.ArgumentMatchers.any(Page.class),
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq(2)
        )).thenReturn(page);

        AiAgentTaskVO vo = new AiAgentTaskVO();
        vo.setId(100L);
        vo.setAgentId(10L);
        when(aiModelConvert.toAgentTaskVO(task)).thenReturn(vo);

        AiAgentDefinition definition = new AiAgentDefinition();
        definition.setId(10L);
        definition.setName("writer-agent");
        when(aiAgentDefinitionRepository.getById(10L)).thenReturn(definition);

        PageResult<AiAgentTaskVO> result = service.pageMyTasks(userId, query);

        assertEquals(1L, result.getTotal());
        assertEquals(2L, result.getCurrent());
        assertEquals(100L, result.getSize());
        assertEquals("writer-agent", result.getRecords().get(0).getAgentName());

        ArgumentCaptor<Page<AiAgentTask>> pageCaptor =
                ArgumentCaptor.forClass(Page.class);
        verify(aiAgentTaskRepository).pageByUserIdAndStatus(
                pageCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq(2)
        );
        assertEquals(2L, pageCaptor.getValue().getCurrent());
        assertEquals(100L, pageCaptor.getValue().getSize());
    }

    @Test
    void createTaskShouldDeliverSuccessNotificationWithJumpData() {
        Long userId = 1L;
        AiAgentDefinition definition = new AiAgentDefinition();
        definition.setId(10L);
        definition.setName("writer-agent");
        definition.setChannelConfigId(100L);
        definition.setEnabled(1);

        AiChannelConfig channelConfig = new AiChannelConfig();
        channelConfig.setId(100L);

        AiModelCallResult callResult = new AiModelCallResult();
        callResult.setSuccess(true);
        callResult.setContent("完成的内容");
        callResult.setTotalTokens(50);

        when(aiAgentDefinitionRepository.getById(10L)).thenReturn(definition);
        when(aiChannelConfigRepository.getById(100L)).thenReturn(channelConfig);
        when(aiModelClient.chat(any(), any(), any(), any())).thenReturn(callResult);
        when(aiAgentTaskRepository.save(any(AiAgentTask.class))).thenAnswer(inv -> {
            AiAgentTask t = inv.getArgument(0);
            t.setId(42L);
            return true;
        });
        when(aiModelConvert.toAgentTaskVO(any(AiAgentTask.class))).thenReturn(new AiAgentTaskVO());

        AiAgentTaskCreateRequest request = new AiAgentTaskCreateRequest();
        request.setAgentId(10L);
        request.setInputContent("帮我写一篇文章");

        service.createTask(userId, request);

        verify(notificationDeliveryService).deliverAfterCommit(
                eq(userId),
                eq(NotificationTypeEnum.AI_TASK_DONE),
                eq("Agent 任务完成"),
                eq("writer-agent 任务已完成"),
                isNull(),
                eq("ai_agent_task"),
                eq(42L),
                eq("/ai/agents/tasks/42"));
    }

    @Test
    void createTaskShouldDeliverFailureNotificationWhenModelFails() {
        Long userId = 1L;
        AiAgentDefinition definition = new AiAgentDefinition();
        definition.setId(10L);
        definition.setName("writer-agent");
        definition.setChannelConfigId(100L);
        definition.setEnabled(1);

        AiChannelConfig channelConfig = new AiChannelConfig();
        channelConfig.setId(100L);

        AiModelCallResult callResult = new AiModelCallResult();
        callResult.setSuccess(false);
        callResult.setErrorMessage("模型调用超时");

        when(aiAgentDefinitionRepository.getById(10L)).thenReturn(definition);
        when(aiChannelConfigRepository.getById(100L)).thenReturn(channelConfig);
        when(aiModelClient.chat(any(), any(), any(), any())).thenReturn(callResult);
        when(aiAgentTaskRepository.save(any(AiAgentTask.class))).thenAnswer(inv -> {
            AiAgentTask t = inv.getArgument(0);
            t.setId(43L);
            return true;
        });
        when(aiModelConvert.toAgentTaskVO(any(AiAgentTask.class))).thenReturn(new AiAgentTaskVO());

        AiAgentTaskCreateRequest request = new AiAgentTaskCreateRequest();
        request.setAgentId(10L);
        request.setInputContent("帮我写一篇文章");

        service.createTask(userId, request);

        verify(notificationDeliveryService).deliverAfterCommit(
                eq(userId),
                eq(NotificationTypeEnum.AI_TASK_DONE),
                eq("Agent 任务失败"),
                eq("writer-agent 任务执行失败"),
                isNull(),
                eq("ai_agent_task"),
                eq(43L),
                eq("/ai/agents/tasks/43"));
    }

    @Test
    void createTaskShouldDeliverFailureNotificationOnException() {
        Long userId = 1L;
        AiAgentDefinition definition = new AiAgentDefinition();
        definition.setId(10L);
        definition.setName("writer-agent");
        definition.setChannelConfigId(100L);
        definition.setEnabled(1);

        AiChannelConfig channelConfig = new AiChannelConfig();
        channelConfig.setId(100L);

        when(aiAgentDefinitionRepository.getById(10L)).thenReturn(definition);
        when(aiChannelConfigRepository.getById(100L)).thenReturn(channelConfig);
        when(aiModelClient.chat(any(), any(), any(), any())).thenThrow(new RuntimeException("连接中断"));
        when(aiAgentTaskRepository.save(any(AiAgentTask.class))).thenAnswer(inv -> {
            AiAgentTask t = inv.getArgument(0);
            t.setId(44L);
            return true;
        });
        when(aiModelConvert.toAgentTaskVO(any(AiAgentTask.class))).thenReturn(new AiAgentTaskVO());

        AiAgentTaskCreateRequest request = new AiAgentTaskCreateRequest();
        request.setAgentId(10L);
        request.setInputContent("帮我写一篇文章");

        service.createTask(userId, request);

        verify(notificationDeliveryService).deliverAfterCommit(
                eq(userId),
                eq(NotificationTypeEnum.AI_TASK_DONE),
                eq("Agent 任务失败"),
                eq("任务执行异常"),
                isNull(),
                eq("ai_agent_task"),
                eq(44L),
                eq("/ai/agents/tasks/44"));
    }
}
