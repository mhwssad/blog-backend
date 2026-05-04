package com.cybzacg.blogbackend.module.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.ai.AiAgentDefinition;
import com.cybzacg.blogbackend.module.ai.convert.AiModelConvert;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentDefinitionPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiAgentDefinitionVO;
import com.cybzacg.blogbackend.module.ai.repository.AiAgentDefinitionRepository;
import com.cybzacg.blogbackend.module.ai.service.impl.AiAgentDefinitionAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AiAgentDefinitionAdminServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class AiAgentDefinitionAdminServiceImplTest {

    @Mock
    private AiAgentDefinitionRepository aiAgentDefinitionRepository;
    @Mock
    private AiModelConvert aiModelConvert;

    private AiAgentDefinitionAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiAgentDefinitionAdminServiceImpl(
                aiAgentDefinitionRepository,
                aiModelConvert
        );
    }

    @Test
    void pageDefinitionsShouldNormalizePaginationAndReturnPageMetadata() {
        AiAgentDefinitionPageQuery query = new AiAgentDefinitionPageQuery();
        query.setCurrent(0L);
        query.setSize(200L);
        query.setKeyword("agent");
        query.setEnabled(1);

        AiAgentDefinition definition = new AiAgentDefinition();
        definition.setId(1L);
        definition.setName("writer-agent");

        Page<AiAgentDefinition> page = new Page<>(1L, 100L);
        page.setTotal(1L);
        page.setRecords(List.of(definition));
        when(aiAgentDefinitionRepository.page(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);

        AiAgentDefinitionVO vo = new AiAgentDefinitionVO();
        vo.setId(1L);
        vo.setName("writer-agent");
        when(aiModelConvert.toAgentDefinitionVO(definition)).thenReturn(vo);

        PageResult<AiAgentDefinitionVO> result = service.pageDefinitions(query);

        assertEquals(1L, result.getTotal());
        assertEquals(1L, result.getCurrent());
        assertEquals(100L, result.getSize());
        assertEquals(List.of(vo), result.getRecords());

        ArgumentCaptor<Page<AiAgentDefinition>> pageCaptor =
                ArgumentCaptor.forClass(Page.class);
        verify(aiAgentDefinitionRepository).page(
                pageCaptor.capture(),
                any(LambdaQueryWrapper.class)
        );
        assertEquals(1L, pageCaptor.getValue().getCurrent());
        assertEquals(100L, pageCaptor.getValue().getSize());
    }
}
