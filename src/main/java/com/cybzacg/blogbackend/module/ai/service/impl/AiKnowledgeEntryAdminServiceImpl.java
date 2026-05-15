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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI 知识条目后台管理服务实现。
 *
 * <p>负责知识条目的分页查询、详情获取和状态管理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiKnowledgeEntryAdminServiceImpl implements AiKnowledgeEntryAdminService {

    private final AiKnowledgeEntryRepository aiKnowledgeEntryRepository;
    private final AiKnowledgeSourceConfigRepository aiKnowledgeSourceConfigRepository;
    private final AiModelConvert aiModelConvert;

    /**
     * 分页查询知识条目列表。
     *
     * @param query 分页查询参数，支持按来源类型、状态和关键词过滤
     * @return 分页结果，包含知识条目 VO 列表
     */
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

    /**
     * 获取指定知识条目的详情。
     *
     * @param id 知识条目 ID
     * @return 知识条目 VO
     * @throws com.cybzacg.blogbackend.exception.BusinessException 条目不存在时抛出
     */
    @Override
    public AiKnowledgeEntryVO getEntry(Long id) {
        AiKnowledgeEntry entry = ExceptionThrowerCore.requireNonNull(
                aiKnowledgeEntryRepository.getById(id),
                ResultErrorCode.AI_KNOWLEDGE_ENTRY_NOT_FOUND);
        return aiModelConvert.toKnowledgeEntryVO(entry);
    }

    /**
     * 更新知识条目的状态（如启用、禁用、标记过期等）。
     *
     * @param id         知识条目 ID
     * @param status     目标状态值，必须为 {@link AiKnowledgeEntryStatusEnum} 中的合法值
     * @param operatorId 操作人 ID
     * @throws com.cybzacg.blogbackend.exception.BusinessException 状态值非法或条目不存在时抛出
     */
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
        log.info("知识条目状态已更新: id={}, status={}", id, status);
    }

    /**
     * 监听内容变更事件，同步更新对应知识条目的状态。
     *
     * <p>根据事件动作类型执行不同策略：
     * <ul>
     *   <li>PUBLISH / UPDATE / RESTORE：标记为待同步（OUTDATED）</li>
     *   <li>HIDE：标记为已禁用（DISABLED）</li>
     *   <li>DELETE：标记为已删除（DELETED）</li>
     * </ul>
     * 仅当对应知识源配置已启用时才处理。
     *
     * @param event 内容变更事件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onContentChange(ContentChangeEvent event) {
        // 不识别的来源类型直接忽略
        if (AiKnowledgeSourceTypeEnum.fromCode(event.getSourceType()) == null) {
            return;
        }
        AiKnowledgeSourceConfig config = aiKnowledgeSourceConfigRepository.findBySourceType(event.getSourceType());
        // 知识源未配置或未启用时忽略该事件
        if (config == null || !Integer.valueOf(1).equals(config.getEnabled())) {
            return;
        }

        AiKnowledgeEntry entry = aiKnowledgeEntryRepository.findBySource(event.getSourceType(), event.getSourceId());
        ContentChangeAction action = event.getAction();

        // 根据内容变更动作类型驱动知识条目状态流转
        switch (action) {
            case PUBLISH, UPDATE, RESTORE -> {
                // 内容发布/更新/恢复：条目不存在则新建，否则标记为待同步
                if (entry == null) {
                    entry = new AiKnowledgeEntry();
                    entry.setSourceType(event.getSourceType());
                    entry.setSourceId(event.getSourceId());
                    entry.setAuthorId(event.getAuthorId());
                    entry.setStatus(AiKnowledgeEntryStatusEnum.OUTDATED.getValue());
                    entry.setVersion(0);
                    entry.setChunkCount(0);
                    aiKnowledgeEntryRepository.save(entry);
                    log.info("新建知识条目: sourceType={}, sourceId={}", event.getSourceType(), event.getSourceId());
                } else if (!AiKnowledgeEntryStatusEnum.DELETED.getValue().equals(entry.getStatus())) {
                    entry.setStatus(AiKnowledgeEntryStatusEnum.OUTDATED.getValue());
                    aiKnowledgeEntryRepository.updateById(entry);
                    log.info("知识条目标记为待同步: id={}", entry.getId());
                }
            }
            case HIDE -> {
                // 内容隐藏：禁用对应知识条目
                if (entry != null && !AiKnowledgeEntryStatusEnum.DELETED.getValue().equals(entry.getStatus())) {
                    entry.setStatus(AiKnowledgeEntryStatusEnum.DISABLED.getValue());
                    aiKnowledgeEntryRepository.updateById(entry);
                    log.info("知识条目已禁用（内容隐藏）: id={}", entry.getId());
                }
            }
            case DELETE -> {
                // 内容删除：将知识条目标记为已删除
                if (entry != null) {
                    entry.setStatus(AiKnowledgeEntryStatusEnum.DELETED.getValue());
                    aiKnowledgeEntryRepository.updateById(entry);
                    log.info("知识条目已标记删除: id={}", entry.getId());
                }
            }
        }
    }
}
