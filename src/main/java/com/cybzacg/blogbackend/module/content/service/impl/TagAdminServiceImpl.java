package com.cybzacg.blogbackend.module.content.service.impl;

import com.cybzacg.blogbackend.domain.SysTag;
import com.cybzacg.blogbackend.domain.SysTagRelation;
import com.cybzacg.blogbackend.enums.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.content.convert.ContentModelMapper;
import com.cybzacg.blogbackend.module.content.model.admin.TagSaveRequest;
import com.cybzacg.blogbackend.module.content.model.admin.TagVO;
import com.cybzacg.blogbackend.module.content.service.SysTagRelationService;
import com.cybzacg.blogbackend.module.content.service.SysTagService;
import com.cybzacg.blogbackend.module.content.service.TagAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 标签后台管理服务实现。
 *
 * <p>负责标签查询、创建、修改、删除，以及删除前的关联关系校验。
 */
@Service
@RequiredArgsConstructor
public class TagAdminServiceImpl implements TagAdminService {
    private final SysTagService sysTagService;
    private final SysTagRelationService sysTagRelationService;
    private final ContentModelMapper contentModelMapper;

    @Override
    public List<TagVO> listTags() {
        return sysTagService.lambdaQuery()
                .orderByDesc(SysTag::getId)
                .list()
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
        SysTag tag = new SysTag();
        tag.setName(request.getName().trim());
        tag.setColor(trim(request.getColor()));
        sysTagService.save(tag);
        return contentModelMapper.toTagVO(tag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagVO updateTag(Long id, TagSaveRequest request) {
        SysTag tag = getTagOrThrow(id);
        validateNameUnique(id, request.getName());
        tag.setName(request.getName().trim());
        tag.setColor(trim(request.getColor()));
        sysTagService.updateById(tag);
        return contentModelMapper.toTagVO(tag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long id) {
        getTagOrThrow(id);
        boolean bound = sysTagRelationService.lambdaQuery().eq(SysTagRelation::getTagId, id).exists();
        if (bound) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "当前标签已绑定目标，无法删除");
        }
        sysTagService.removeById(id);
    }

    /**
     * 校验标签名称唯一，避免后台维护时出现重名标签。
     */
    private void validateNameUnique(Long currentId, String name) {
        boolean exists = sysTagService.lambdaQuery()
                .eq(SysTag::getName, name.trim())
                .ne(currentId != null, SysTag::getId, currentId)
                .exists();
        if (exists) {
            throw new BusinessException(ResultErrorCode.DATA_ALREADY_EXISTS.getCode(), "标签名称已存在");
        }
    }

    /**
     * 按 ID 获取标签，不存在时抛出统一业务异常。
     */
    private SysTag getTagOrThrow(Long id) {
        SysTag tag = sysTagService.getById(id);
        if (tag == null) {
            throw new BusinessException(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), "标签不存在");
        }
        return tag;
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : value;
    }
}
