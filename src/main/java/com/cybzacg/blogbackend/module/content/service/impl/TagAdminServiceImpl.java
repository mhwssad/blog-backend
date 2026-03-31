package com.cybzacg.blogbackend.module.content.service.impl;

import com.cybzacg.blogbackend.domain.SysTag;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.admin.TagSaveRequest;
import com.cybzacg.blogbackend.module.content.model.admin.TagVO;
import com.cybzacg.blogbackend.module.content.repository.SysTagRelationRepository;
import com.cybzacg.blogbackend.module.content.repository.SysTagRepository;
import com.cybzacg.blogbackend.module.content.service.TagAdminService;
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
    private final ContentModelMapper contentModelMapper;

    @Override
    public List<TagVO> listTags() {
        return sysTagRepository.findAllOrderByIdDesc()
                .stream()
                .map(contentModelMapper::toTagVO)
                .toList();
    }

    @Override
    public TagVO getTag(Long id) {
        return contentModelMapper.toTagVO(getTagOrThrow(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagVO createTag(TagSaveRequest request) {
        validateNameUnique(null, request.getName());
        SysTag tag = contentModelMapper.toTag(request);
        sysTagRepository.save(tag);
        return contentModelMapper.toTagVO(tag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagVO updateTag(Long id, TagSaveRequest request) {
        SysTag tag = getTagOrThrow(id);
        validateNameUnique(id, request.getName());
        contentModelMapper.updateTag(request, tag);
        sysTagRepository.updateById(tag);
        return contentModelMapper.toTagVO(tag);
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
