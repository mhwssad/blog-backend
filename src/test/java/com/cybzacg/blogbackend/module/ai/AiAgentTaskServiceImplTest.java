package com.cybzacg.blogbackend.module.ai;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.ai.AiAgentDefinition;
import com.cybzacg.blogbackend.domain.ai.AiAgentTask;
import com.cybzacg.blogbackend.module.ai.convert.AiModelConvert;
import com.cybzacg.blogbackend.module.ai.model.user.AiAgentTaskPageQuery;
import com.cybzacg.blogbackend.module.ai.model.user.AiAgentTaskVO;
import com.cybzacg.blogbackend.module.ai.repository.AiAgentDefinitionRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiAgentTaskLogRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiAgentTaskRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiChannelConfigRepository;
import com.cybzacg.blogbackend.module.ai.service.AiModelClient;
import com.cybzacg.blogbackend.module.ai.service.AiQuotaService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
