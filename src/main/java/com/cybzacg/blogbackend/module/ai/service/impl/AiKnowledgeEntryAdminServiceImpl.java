package com.cybzacg.blogbackend.module.ai.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.dto.domain.ai.AiKnowledgeEntry;
import com.cybzacg.blogbackend.dto.domain.ai.AiKnowledgeSourceConfig;
import com.cybzacg.blogbackend.dto.repository.ai.AiKnowledgeEntryRepository;
import com.cybzacg.blogbackend.dto.repository.ai.AiKnowledgeSourceConfigRepository;
import com.cybzacg.blogbackend.enums.ai.AiKnowledgeEntryStatusEnum;
import com.cybzacg.blogbackend.enums.ai.AiKnowledgeSourceTypeEnum;
import com.cybzacg.blogbackend.enums.ai.ContentChangeAction;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.ai.convert.AiModelConvert;
import com.cybzacg.blogbackend.module.ai.event.ContentChangeEvent;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeEntryPageQuery;
import com.cybzacg.blogbackend.module.ai.model.admin.AiKnowledgeEntryVO;
import com.cybzacg.blogbackend.module.ai.service.AiKnowledgeEntryAdminService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI 知识条目后台管理服务实现。
 *
 * <p>负责知识条目的分页查询、详情获取和状态管理。
 */
@Service
@RequiredArgsConstructor
public class AiKnowledgeEntryAdminServiceImpl implements AiKnowledgeEntryAdminService {

    private final AiKnowledgeEntryRepository aiKnowledgeEntryRepository;
    private final AiKnowledgeSourceConfigRepository aiKnowledgeSourceConfigRepository;
    private final AiModelConvert aiModelConvert;

    @Override
    public PageResult<AiKnowledgeEntryVO> listEntries(AiKnowledgeEntryPageQuery query) {
        long current = PaginationUtils.normalizeCurrent(query.getCurrent());
        long size = PaginationUtils.normalizeSize(query.getSize(), 20L, 100L);

        Page<AiKnowledgeEntry> page = aiKnowledgeEntryRepository.pageByQuery(
                query.getSourceType(), query.getStatus(), query.getKeyword(), current, size);

        List<AiKnowledgeEntryVO> records = page.getRecords().stream()
                .map(aiModelConvert::toKnowledgeEntryVO)
                .toList();
        return PageResult.of(page, records);
    }

    @Override
    public AiKnowledgeEntryVO getEntry(Long id) {
        AiKnowledgeEntry entry = ExceptionThrowerCore.requireNonNull(
                aiKnowledgeEntryRepository.getById(id),
                ResultErrorCode.AI_KNOWLEDGE_ENTRY_NOT_FOUND);
        return aiModelConvert.toKnowledgeEntryVO(entry);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEntryStatus(Long id, Integer status, Long operatorId) {
        ExceptionThrowerCore.throwBusinessIf(
                !AiKnowledgeEntryStatusEnum.contains(status),
                ResultErrorCode.AI_KNOWLEDGE_ENTRY_STATUS_INVALID);

        AiKnowledgeEntry entry = ExceptionThrowerCore.requireNonNull(
                aiKnowledgeEntryRepository.getById(id),
                ResultErrorCode.AI_KNOWLEDGE_ENTRY_NOT_FOUND);

        entry.setStatus(status);
        aiKnowledgeEntryRepository.updateById(entry);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onContentChange(ContentChangeEvent event) {
        if (AiKnowledgeSourceTypeEnum.fromCode(event.getSourceType()) == null) {
            return;
        }
        AiKnowledgeSourceConfig config = aiKnowledgeSourceConfigRepository.findBySourceType(event.getSourceType());
        if (config == null || !Integer.valueOf(1).equals(config.getEnabled())) {
            return;
        }

        AiKnowledgeEntry entry = aiKnowledgeEntryRepository.findBySource(event.getSourceType(), event.getSourceId());
        ContentChangeAction action = event.getAction();

        switch (action) {
            case PUBLISH, UPDATE, RESTORE -> {
                if (entry == null) {
                    entry = new AiKnowledgeEntry();
                    entry.setSourceType(event.getSourceType());
                    entry.setSourceId(event.getSourceId());
                    entry.setAuthorId(event.getAuthorId());
                    entry.setStatus(AiKnowledgeEntryStatusEnum.OUTDATED.getValue());
                    entry.setVersion(0);
                    entry.setChunkCount(0);
                    aiKnowledgeEntryRepository.save(entry);
                } else if (!AiKnowledgeEntryStatusEnum.DELETED.getValue().equals(entry.getStatus())) {
                    entry.setStatus(AiKnowledgeEntryStatusEnum.OUTDATED.getValue());
                    aiKnowledgeEntryRepository.updateById(entry);
                }
            }
            case HIDE -> {
                if (entry != null && !AiKnowledgeEntryStatusEnum.DELETED.getValue().equals(entry.getStatus())) {
                    entry.setStatus(AiKnowledgeEntryStatusEnum.DISABLED.getValue());
                    aiKnowledgeEntryRepository.updateById(entry);
                }
            }
            case DELETE -> {
                if (entry != null) {
                    entry.setStatus(AiKnowledgeEntryStatusEnum.DELETED.getValue());
                    aiKnowledgeEntryRepository.updateById(entry);
                }
            }
        }
    }
}
