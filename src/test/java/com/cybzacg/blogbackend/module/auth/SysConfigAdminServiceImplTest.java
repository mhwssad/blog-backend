package com.cybzacg.blogbackend.module.auth;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysConfig;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.convert.SysConfigModelMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysConfigAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysConfigPageQuery;
import com.cybzacg.blogbackend.module.auth.model.admin.SysConfigSaveRequest;
import com.cybzacg.blogbackend.module.auth.service.SysConfigService;
import com.cybzacg.blogbackend.module.auth.service.impl.SysConfigAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysConfigAdminServiceImplTest {
    @Mock
    private SysConfigService sysConfigService;
    @Mock
    private SysConfigModelMapper sysConfigModelMapper;
    @Mock
    private LambdaQueryChainWrapper<SysConfig> configQuery;

    private SysConfigAdminServiceImpl sysConfigAdminService;

    @BeforeEach
    void setUp() {
        sysConfigAdminService = new SysConfigAdminServiceImpl(sysConfigService, sysConfigModelMapper);
    }

    @Test
    void pageConfigsShouldReturnMappedPageResult() {
        SysConfigPageQuery query = new SysConfigPageQuery();
        query.setCurrent(1L);
        query.setSize(10L);
        query.setConfigName("站点");
        query.setConfigKey("site.");
        query.setCreateTimeStart(new Date(1_000L));
        query.setCreateTimeEnd(new Date(2_000L));

        SysConfig config = config(1L, "站点标题", "site.title", "Blog");
        Page<SysConfig> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(config));

        when(sysConfigService.lambdaQuery()).thenReturn(configQuery);
        stubConfigQueryChain();
        when(configQuery.page(any())).thenReturn(page);

        SysConfigAdminVO expected = configVO(1L, "站点标题", "site.title", "Blog");
        when(sysConfigModelMapper.toConfigVO(config)).thenReturn(expected);

        PageResult<SysConfigAdminVO> result = sysConfigAdminService.pageConfigs(query);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("site.title", result.getRecords().get(0).getConfigKey());
        verify(configQuery).page(any());
    }

    @Test
    void getConfigShouldReturnMappedVO() {
        SysConfig config = config(1L, "站点标题", "site.title", "Blog");
        when(sysConfigService.getById(1L)).thenReturn(config);

        SysConfigAdminVO expected = configVO(1L, "站点标题", "site.title", "Blog");
        when(sysConfigModelMapper.toConfigVO(config)).thenReturn(expected);

        SysConfigAdminVO result = sysConfigAdminService.getConfig(1L);

        assertEquals(1L, result.getId());
        assertEquals("site.title", result.getConfigKey());
    }

    @Test
    void getConfigShouldThrowWhenConfigDeleted() {
        SysConfig config = config(1L, "站点标题", "site.title", "Blog");
        config.setIsDeleted(1);
        when(sysConfigService.getById(1L)).thenReturn(config);

        BusinessException exception = assertThrows(BusinessException.class, () -> sysConfigAdminService.getConfig(1L));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("配置不存在", exception.getMessage());
    }

    @Test
    void createConfigShouldSaveEvictCacheAndReturnVO() {
        SysConfigSaveRequest request = new SysConfigSaveRequest();
        request.setConfigName("站点标题");
        request.setConfigKey(" site.title ");
        request.setConfigValue("Blog");
        request.setRemark("remark");

        when(sysConfigService.lambdaQuery()).thenReturn(configQuery);
        stubExistsQueryChain();
        when(configQuery.exists()).thenReturn(false);

        when(sysConfigModelMapper.toConfigVO(any(SysConfig.class))).thenAnswer(invocation -> {
            SysConfig saved = invocation.getArgument(0);
            return configVO(saved.getId(), saved.getConfigName(), saved.getConfigKey(), saved.getConfigValue());
        });

        ArgumentCaptor<SysConfig> configCaptor = ArgumentCaptor.forClass(SysConfig.class);

        SysConfigAdminVO result = sysConfigAdminService.createConfig(request);

        verify(sysConfigService).save(configCaptor.capture());
        SysConfig saved = configCaptor.getValue();
        assertEquals("站点标题", saved.getConfigName());
        assertEquals("site.title", saved.getConfigKey());
        assertEquals("Blog", saved.getConfigValue());
        assertEquals(0, saved.getIsDeleted());
        verify(sysConfigService).evictConfigCache("site.title");
        assertEquals("site.title", result.getConfigKey());
    }

    @Test
    void createConfigShouldThrowWhenConfigKeyExists() {
        SysConfigSaveRequest request = new SysConfigSaveRequest();
        request.setConfigName("站点标题");
        request.setConfigKey("site.title");
        request.setConfigValue("Blog");

        when(sysConfigService.lambdaQuery()).thenReturn(configQuery);
        stubExistsQueryChain();
        when(configQuery.exists()).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> sysConfigAdminService.createConfig(request));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("配置键已存在", exception.getMessage());
        verify(sysConfigService, never()).save(any(SysConfig.class));
    }

    @Test
    void updateConfigShouldUpdateEvictBothCacheKeysAndReturnVO() {
        SysConfig existing = config(1L, "站点标题", "site.title", "Blog");
        when(sysConfigService.getById(1L)).thenReturn(existing);
        when(sysConfigService.lambdaQuery()).thenReturn(configQuery);
        stubExistsQueryChain();
        when(configQuery.exists()).thenReturn(false);

        SysConfigSaveRequest request = new SysConfigSaveRequest();
        request.setConfigName("新标题");
        request.setConfigKey("site.name");
        request.setConfigValue("New Blog");
        request.setRemark("remark");

        when(sysConfigModelMapper.toConfigVO(existing)).thenAnswer(invocation ->
                configVO(existing.getId(), existing.getConfigName(), existing.getConfigKey(), existing.getConfigValue()));

        SysConfigAdminVO result = sysConfigAdminService.updateConfig(1L, request);

        assertEquals("新标题", existing.getConfigName());
        assertEquals("site.name", existing.getConfigKey());
        assertEquals("New Blog", existing.getConfigValue());
        verify(sysConfigService).updateById(existing);
        verify(sysConfigService).evictConfigCache("site.title");
        verify(sysConfigService).evictConfigCache("site.name");
        assertEquals("site.name", result.getConfigKey());
    }

    @Test
    void deleteConfigShouldSoftDeleteAndEvictCache() {
        SysConfig existing = config(1L, "站点标题", "site.title", "Blog");
        when(sysConfigService.getById(1L)).thenReturn(existing);

        sysConfigAdminService.deleteConfig(1L);

        assertEquals(1, existing.getIsDeleted());
        verify(sysConfigService).updateById(existing);
        verify(sysConfigService).evictConfigCache("site.title");
    }

    @Test
    void getValueByKeyShouldDelegateToSysConfigService() {
        when(sysConfigService.getValueByKey("site.title")).thenReturn("Blog");

        String result = sysConfigAdminService.getValueByKey("site.title");

        assertEquals("Blog", result);
        verify(sysConfigService).getValueByKey("site.title");
    }

    private void stubConfigQueryChain() {
        when(configQuery.like(anyBoolean(), any(SFunction.class), any())).thenReturn(configQuery);
        when(configQuery.ge(anyBoolean(), any(SFunction.class), any())).thenReturn(configQuery);
        when(configQuery.le(anyBoolean(), any(SFunction.class), any())).thenReturn(configQuery);
        when(configQuery.eq(any(SFunction.class), any())).thenReturn(configQuery);
        when(configQuery.orderByDesc(any(SFunction.class))).thenReturn(configQuery);
    }

    private void stubExistsQueryChain() {
        when(configQuery.eq(any(SFunction.class), any())).thenReturn(configQuery);
        when(configQuery.ne(anyBoolean(), any(SFunction.class), any())).thenReturn(configQuery);
    }

    private SysConfig config(Long id, String name, String key, String value) {
        SysConfig config = new SysConfig();
        config.setId(id);
        config.setConfigName(name);
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setIsDeleted(0);
        return config;
    }

    private SysConfigAdminVO configVO(Long id, String name, String key, String value) {
        SysConfigAdminVO vo = new SysConfigAdminVO();
        vo.setId(id);
        vo.setConfigName(name);
        vo.setConfigKey(key);
        vo.setConfigValue(value);
        return vo;
    }
}
