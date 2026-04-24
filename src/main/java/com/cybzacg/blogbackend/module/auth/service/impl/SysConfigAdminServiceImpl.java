package com.cybzacg.blogbackend.module.auth.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysConfig;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.convert.SysConfigModelMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysConfigAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysConfigPageQuery;
import com.cybzacg.blogbackend.module.auth.model.admin.SysConfigSaveRequest;
import com.cybzacg.blogbackend.module.auth.repository.SysConfigRepository;
import com.cybzacg.blogbackend.module.auth.service.SysConfigAdminService;
import com.cybzacg.blogbackend.module.auth.service.SysConfigService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统配置后台管理服务实现。
 *
 * <p>负责系统配置分页查询、增删改，以及配置缓存失效处理。
 */
@Service
@RequiredArgsConstructor
public class SysConfigAdminServiceImpl implements SysConfigAdminService {
    private final SysConfigRepository sysConfigRepository;
    private final SysConfigService sysConfigService;
    private final SysConfigModelMapper sysConfigModelMapper;

    @Override
    public PageResult<SysConfigAdminVO> pageConfigs(SysConfigPageQuery query) {
        Page<SysConfig> page = sysConfigRepository.pageByAdminConditions(query);
        List<SysConfigAdminVO> records = page.getRecords().stream()
                .map(sysConfigModelMapper::toConfigVO)
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public SysConfigAdminVO getConfig(Long id) {
        return sysConfigModelMapper.toConfigVO(getAvailableConfig(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysConfigAdminVO createConfig(SysConfigSaveRequest request) {
        validateConfigKeyUnique(null, request.getConfigKey());
        SysConfig config = new SysConfig();
        applyFields(config, request);
        config.setIsDeleted(0);
        sysConfigRepository.save(config);
        sysConfigService.evictConfigCache(config.getConfigKey());
        return sysConfigModelMapper.toConfigVO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysConfigAdminVO updateConfig(Long id, SysConfigSaveRequest request) {
        SysConfig config = getAvailableConfig(id);
        String oldConfigKey = config.getConfigKey();
        validateConfigKeyUnique(id, request.getConfigKey());
        applyFields(config, request);
        sysConfigRepository.updateById(config);
        sysConfigService.evictConfigCache(oldConfigKey);
        sysConfigService.evictConfigCache(config.getConfigKey());
        return sysConfigModelMapper.toConfigVO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        SysConfig config = getAvailableConfig(id);
        config.setIsDeleted(1);
        sysConfigRepository.updateById(config);
        sysConfigService.evictConfigCache(config.getConfigKey());
    }

    @Override
    public String getValueByKey(String configKey) {
        return sysConfigService.getValueByKey(configKey);
    }

    private void applyFields(SysConfig config, SysConfigSaveRequest request) {
        config.setConfigName(StrUtils.normalize(request.getConfigName()));
        config.setConfigKey(StrUtils.normalize(request.getConfigKey()));
        config.setConfigValue(request.getConfigValue());
        config.setRemark(request.getRemark());
    }

    private void validateConfigKeyUnique(Long currentId, String configKey) {
        if (sysConfigRepository.existsActiveByConfigKey(StrUtils.normalize(configKey), currentId)) {
            ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.ILLEGAL_ARGUMENT, "配置键已存在");
        }
    }

    private SysConfig getAvailableConfig(Long id) {
        SysConfig config = sysConfigRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIf(config == null || Integer.valueOf(1).equals(config.getIsDeleted()), ResultErrorCode.ILLEGAL_ARGUMENT, "配置不存在");
        return config;
    }
}
