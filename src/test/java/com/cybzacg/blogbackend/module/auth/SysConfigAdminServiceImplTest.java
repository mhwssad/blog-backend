package com.cybzacg.blogbackend.module.auth;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysConfig;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.convert.SysConfigModelMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysConfigAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysConfigPageQuery;
import com.cybzacg.blogbackend.module.auth.model.admin.SysConfigSaveRequest;
import com.cybzacg.blogbackend.module.auth.repository.SysConfigRepository;
import com.cybzacg.blogbackend.module.auth.service.SysConfigService;
import com.cybzacg.blogbackend.module.auth.service.impl.SysConfigAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysConfigAdminServiceImplTest {
    @Mock
    private SysConfigRepository sysConfigRepository;
    @Mock
    private SysConfigService sysConfigService;
    @Mock
    private SysConfigModelMapper sysConfigModelMapper;

    private SysConfigAdminServiceImpl sysConfigAdminService;

    @BeforeEach
    void setUp() {
        sysConfigAdminService = new SysConfigAdminServiceImpl(sysConfigRepository, sysConfigService, sysConfigModelMapper);
    }

    @Test
    void pageConfigsShouldReturnMappedPageResult() {
        SysConfigPageQuery query = new SysConfigPageQuery();
        query.setCurrent(1L);
        query.setSize(10L);

        SysConfig config = config(1L, "站点标题", "site.title", "Blog");
        Page<SysConfig> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(config));

        when(sysConfigRepository.pageByAdminConditions(query)).thenReturn(page);
        SysConfigAdminVO expected = configVO(1L, "站点标题", "site.title", "Blog");
        when(sysConfigModelMapper.toConfigVO(config)).thenReturn(expected);

        PageResult<SysConfigAdminVO> result = sysConfigAdminService.pageConfigs(query);

        assertEquals(1L, result.getTotal());
        assertEquals("site.title", result.getRecords().get(0).getConfigKey());
    }

    @Test
    void createConfigShouldSaveEvictCacheAndReturnVO() {
        SysConfigSaveRequest request = new SysConfigSaveRequest();
        request.setConfigName("站点标题");
        request.setConfigKey(" site.title ");
        request.setConfigValue("Blog");

        when(sysConfigRepository.existsActiveByConfigKey("site.title", null)).thenReturn(false);
        when(sysConfigModelMapper.toConfigVO(any(SysConfig.class))).thenAnswer(invocation -> {
            SysConfig saved = invocation.getArgument(0);
            return configVO(saved.getId(), saved.getConfigName(), saved.getConfigKey(), saved.getConfigValue());
        });

        SysConfigAdminVO result = sysConfigAdminService.createConfig(request);

        verify(sysConfigRepository).save(any(SysConfig.class));
        verify(sysConfigService).evictConfigCache("site.title");
        assertEquals("site.title", result.getConfigKey());
    }

    @Test
    void updateConfigShouldUpdateAndEvictBothKeys() {
        SysConfig existing = config(1L, "站点标题", "site.title", "Blog");
        when(sysConfigRepository.getById(1L)).thenReturn(existing);
        when(sysConfigRepository.existsActiveByConfigKey("site.name", 1L)).thenReturn(false);
        when(sysConfigModelMapper.toConfigVO(existing)).thenAnswer(invocation ->
                configVO(existing.getId(), existing.getConfigName(), existing.getConfigKey(), existing.getConfigValue()));

        SysConfigSaveRequest request = new SysConfigSaveRequest();
        request.setConfigName("新标题");
        request.setConfigKey("site.name");
        request.setConfigValue("New Blog");

        SysConfigAdminVO result = sysConfigAdminService.updateConfig(1L, request);

        verify(sysConfigRepository).updateById(existing);
        verify(sysConfigService).evictConfigCache("site.title");
        verify(sysConfigService).evictConfigCache("site.name");
        assertEquals("site.name", result.getConfigKey());
    }

    @Test
    void deleteConfigShouldSoftDeleteAndEvictCache() {
        SysConfig existing = config(1L, "站点标题", "site.title", "Blog");
        when(sysConfigRepository.getById(1L)).thenReturn(existing);

        sysConfigAdminService.deleteConfig(1L);

        assertEquals(1, existing.getIsDeleted());
        verify(sysConfigRepository).updateById(existing);
        verify(sysConfigService).evictConfigCache("site.title");
    }

    @Test
    void createConfigShouldThrowWhenConfigKeyExists() {
        SysConfigSaveRequest request = new SysConfigSaveRequest();
        request.setConfigKey("site.title");

        when(sysConfigRepository.existsActiveByConfigKey("site.title", null)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> sysConfigAdminService.createConfig(request));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        verify(sysConfigRepository, never()).save(any(SysConfig.class));
    }

    @Test
    void getValueByKeyShouldDelegateToSysConfigService() {
        when(sysConfigService.getValueByKey("site.title")).thenReturn("Blog");

        String result = sysConfigAdminService.getValueByKey("site.title");

        assertEquals("Blog", result);
        verify(sysConfigService).getValueByKey("site.title");
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
