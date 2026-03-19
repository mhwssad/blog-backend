package com.cybzacg.blogbackend.module.auth.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysConfig;
import com.cybzacg.blogbackend.enums.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.convert.SysConfigModelMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysConfigAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysConfigPageQuery;
import com.cybzacg.blogbackend.module.auth.model.admin.SysConfigSaveRequest;
import com.cybzacg.blogbackend.module.auth.service.SysConfigAdminService;
import com.cybzacg.blogbackend.module.auth.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 系统配置后台管理服务实现。
 *
 * <p>负责系统配置分页查询、增删改，以及配置缓存失效处理。
 */
@Service
@RequiredArgsConstructor
public class SysConfigAdminServiceImpl implements SysConfigAdminService {
    private final SysConfigService sysConfigService;
    private final SysConfigModelMapper sysConfigModelMapper;

    @Override
    public PageResult<SysConfigAdminVO> pageConfigs(SysConfigPageQuery query) {
        Page<SysConfig> page = sysConfigService.lambdaQuery()
                .like(StringUtils.hasText(query.getConfigName()), SysConfig::getConfigName, query.getConfigName())
                .like(StringUtils.hasText(query.getConfigKey()), SysConfig::getConfigKey, query.getConfigKey())
                .ge(query.getCreateTimeStart() != null, SysConfig::getCreateTime, query.getCreateTimeStart())
                .le(query.getCreateTimeEnd() != null, SysConfig::getCreateTime, query.getCreateTimeEnd())
                .eq(SysConfig::getIsDeleted, 0)
                .orderByDesc(SysConfig::getCreateTime)
                .orderByDesc(SysConfig::getId)
                .page(new Page<>(query.getCurrent(), query.getSize()));

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
        sysConfigService.save(config);
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
        sysConfigService.updateById(config);
        sysConfigService.evictConfigCache(oldConfigKey);
        sysConfigService.evictConfigCache(config.getConfigKey());
        return sysConfigModelMapper.toConfigVO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        SysConfig config = getAvailableConfig(id);
        config.setIsDeleted(1);
        sysConfigService.updateById(config);
        sysConfigService.evictConfigCache(config.getConfigKey());
    }

    @Override
    public String getValueByKey(String configKey) {
        return sysConfigService.getValueByKey(configKey);
    }

    /**
     * 将请求中的配置字段统一回填到实体，复用新增和更新流程。
     */
    private void applyFields(SysConfig config, SysConfigSaveRequest request) {
        config.setConfigName(normalize(request.getConfigName()));
        config.setConfigKey(normalize(request.getConfigKey()));
        config.setConfigValue(request.getConfigValue());
        config.setRemark(request.getRemark());
    }

    /**
     * 校验配置键在未删除配置中保持唯一。
     */
    private void validateConfigKeyUnique(Long currentId, String configKey) {
        if (sysConfigService.lambdaQuery()
                .eq(SysConfig::getConfigKey, normalize(configKey))
                .eq(SysConfig::getIsDeleted, 0)
                .ne(currentId != null, SysConfig::getId, currentId)
                .exists()) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "配置键已存在");
        }
    }

    /**
     * 获取有效配置，不存在或已删除时抛出统一业务异常。
     */
    private SysConfig getAvailableConfig(Long id) {
        SysConfig config = sysConfigService.getById(id);
        if (config == null || Integer.valueOf(1).equals(config.getIsDeleted())) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "配置不存在");
        }
        return config;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : value;
    }
}
