package com.cybzacg.blogbackend.module.ai;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.ai.AiAgentDefinition;
import com.cybzacg.blogbackend.domain.ai.AiAgentTask;
import com.cybzacg.blogbackend.module.ai.convert.AiModelConvert;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentTaskAdminPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentTaskAdminVO;
import com.cybzacg.blogbackend.module.ai.repository.AiAgentDefinitionRepository;
import com.cybzacg.blogbackend.module.ai.repository.AiAgentTaskRepository;
import com.cybzacg.blogbackend.module.ai.service.impl.AiAgentTaskAdminServiceImpl;
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
 * AiAgentTaskAdminServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class AiAgentTaskAdminServiceImplTest {

    @Mock
    private AiAgentTaskRepository aiAgentTaskRepository;
    @Mock
    private AiAgentDefinitionRepository aiAgentDefinitionRepository;
    @Mock
    private AiModelConvert aiModelConvert;

    private AiAgentTaskAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiAgentTaskAdminServiceImpl(
                aiAgentTaskRepository,
                aiAgentDefinitionRepository,
                aiModelConvert
        );
    }

    @Test
    void pageTasksShouldNormalizePaginationAndFillAgentName() {
        AiAgentTaskAdminPageQuery query = new AiAgentTaskAdminPageQuery();
        query.setCurrent(null);
        query.setSize(0L);
        query.setAgentId(10L);
        query.setStatus(2);

        AiAgentTask task = new AiAgentTask();
        task.setId(100L);
        task.setAgentId(10L);

        Page<AiAgentTask> page = new Page<>(1L, 20L);
        page.setTotal(1L);
        page.setRecords(List.of(task));
        when(aiAgentTaskRepository.pageByAgentIdAndStatus(
                org.mockito.ArgumentMatchers.any(Page.class),
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(2)
        )).thenReturn(page);

        AiAgentTaskAdminVO vo = new AiAgentTaskAdminVO();
        vo.setId(100L);
        vo.setAgentId(10L);
        when(aiModelConvert.toAgentTaskAdminVO(task)).thenReturn(vo);

        AiAgentDefinition definition = new AiAgentDefinition();
        definition.setId(10L);
        definition.setName("qa-agent");
        when(aiAgentDefinitionRepository.getById(10L)).thenReturn(definition);

        PageResult<AiAgentTaskAdminVO> result = service.pageTasks(query);

        assertEquals(1L, result.getTotal());
        assertEquals(1L, result.getCurrent());
        assertEquals(20L, result.getSize());
        assertEquals("qa-agent", result.getRecords().get(0).getAgentName());

        ArgumentCaptor<Page<AiAgentTask>> pageCaptor =
                ArgumentCaptor.forClass(Page.class);
        verify(aiAgentTaskRepository).pageByAgentIdAndStatus(
                pageCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(2)
        );
        assertEquals(1L, pageCaptor.getValue().getCurrent());
        assertEquals(20L, pageCaptor.getValue().getSize());
    }
}
