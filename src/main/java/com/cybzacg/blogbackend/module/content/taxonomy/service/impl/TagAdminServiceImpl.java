package com.cybzacg.blogbackend.module.content.taxonomy.service.impl;

import com.cybzacg.blogbackend.dto.domain.content.SysTag;
import com.cybzacg.blogbackend.dto.repository.content.SysTagRelationRepository;
import com.cybzacg.blogbackend.dto.repository.content.SysTagRepository;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.content.shared.convert.ContentModelConvert;
import com.cybzacg.blogbackend.module.content.taxonomy.model.admin.TagSaveRequest;
import com.cybzacg.blogbackend.module.content.taxonomy.model.admin.TagVO;
import com.cybzacg.blogbackend.module.content.taxonomy.service.TagAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 标签后台管理服务实现。
 *
 * <p>负责标签查询、创建、修改，以及删除标签时的关联关系清理。
 */
@Service
@RequiredArgsConstructor
public class TagAdminServiceImpl implements TagAdminService {
    private final SysTagRepository sysTagRepository;
    private final SysTagRelationRepository sysTagRelationRepository;
    private final ContentModelConvert contentModelConvert;

    /**
     * 查询全部标签列表，按ID降序返回。
     */
    @Override
    public List<TagVO> listTags() {
        return sysTagRepository.findAllOrderByIdDesc()
                .stream()
                .map(contentModelConvert::toTagVO)
                .toList();
    }

    /**
     * 按ID获取标签详情。
     */
    @Override
    public TagVO getTag(Long id) {
        return contentModelConvert.toTagVO(getTagOrThrow(id));
    }

    /**
     * 创建标签，若同名标签已存在则直接返回。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagVO createTag(TagSaveRequest request) {
        String trimmedName = StrUtils.trim(request.getName());
        SysTag existing = sysTagRepository.findByName(trimmedName);
        if (existing != null) {
            return contentModelConvert.toTagVO(existing);
        }
        SysTag tag = contentModelConvert.toTag(request);
        sysTagRepository.save(tag);
        return contentModelConvert.toTagVO(tag);
    }

    /**
     * 更新标签信息，校验名称唯一后写入。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagVO updateTag(Long id, TagSaveRequest request) {
        SysTag tag = getTagOrThrow(id);
        validateNameUnique(id, request.getName());
        contentModelConvert.updateTag(request, tag);
        sysTagRepository.updateById(tag);
        return contentModelConvert.toTagVO(tag);
    }

    /**
     * 删除标签时同步清理标签-目标关联，避免遗留失效的 sys_tag_relation 记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long id) {
        getTagOrThrow(id);
        sysTagRelationRepository.removeByTagId(id);
        sysTagRepository.removeById(id);
    }

    /**
     * 校验标签名称唯一，避免后台维护时出现重名标签。
     */
    private void validateNameUnique(Long currentId, String name) {
        boolean exists = sysTagRepository.existsByNameExcludingId(StrUtils.trim(name), currentId);
        ExceptionThrowerCore.throwBusinessIf(exists, ResultErrorCode.DATA_ALREADY_EXISTS, "标签名称已存在");
    }

    /**
     * 按 ID 获取标签，不存在时抛出统一业务异常。
     */
    private SysTag getTagOrThrow(Long id) {
        SysTag tag = sysTagRepository.getById(id);
        ExceptionThrowerCore.throwBusinessIfNull(tag, ResultErrorCode.ILLEGAL_ARGUMENT, "标签不存在");
        return tag;
    }
}
