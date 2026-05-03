package com.cybzacg.blogbackend.module.file.service.impl;

import com.cybzacg.blogbackend.domain.file.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.file.FileInfo;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.file.FileReferenceTypeEnum;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileInfoRepository;
import com.cybzacg.blogbackend.module.file.service.FileChatFacadeService;
import com.cybzacg.blogbackend.module.file.service.FileLifecycleService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 文件域对聊天模块暴露的 facade 实现。
 * <p>
 * 该服务封装了文件业务引用与聊天消息的绑定/解绑逻辑，是文件模块与聊天模块之间的唯一耦合点。
 * 其核心职责包括：
 * <ul>
 *   <li>校验文件引用是否可用于发送（临时引用或未绑定消息的聊天引用）</li>
 *   <li>将文件引用收口到指定的聊天消息ID上，支持幂等重入</li>
 *   <li>释放聊天消息下的所有文件引用，并触发文件生命周期同步</li>
 * </ul>
 * </p>
 * <p>
 * 设计原则：
 * <ul>
 *   <li>所有变更操作均在统一事务边界内完成，保证文件业务引用与消息的原子绑定</li>
 *   <li>幂等处理：重复绑定同一消息时直接返回已有引用，不重复创建</li>
 *   <li>解绑时同步触发文件引用计数刷新，驱动文件生命周期状态流转</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileChatFacadeServiceImpl implements FileChatFacadeService {
    private final FileBusinessInfoRepository fileBusinessInfoRepository;
    private final FileInfoRepository fileInfoRepository;
    private final FileLifecycleService fileLifecycleService;


    /**
     * 校验文件业务引用可被聊天消息消费，并返回真实文件信息。
     * <p>
     * 调用方在发送聊天消息前需先通过此方法获取可发送的文件实体。
     * 内部会依次校验：引用归属权、引用类型合法性（仅允许临时引用或未绑定消息的聊天引用）、文件状态。
     * </p>
     *
     * @param userId           当前用户ID，null表示不校验归属
     * @param businessId       文件业务引用ID
     * @param chatReferenceType 聊天引用类型，用于校验引用来源
     * @return 处于正常状态的文件实体
     * @throws BusinessException 引用不存在、无权使用、引用类型不合法或文件已下线
     */
    @Override
    public FileInfo requireSendableChatFile(Long userId, Long businessId, String chatReferenceType) {
        log.debug("[聊天文件] 校验文件可发送性，userId: {}，businessId: {}，chatReferenceType: {}",
                userId, businessId, chatReferenceType);
        FileBusinessInfo sourceReference = requireChatConsumableReference(userId, businessId, chatReferenceType);
        FileInfo fileInfo = requireNormalFile(sourceReference.getFileId());
        log.debug("[聊天文件] 文件可发送，fileId: {}，originalName: {}", fileInfo.getId(), fileInfo.getOriginalName());
        return fileInfo;
    }


    /**
     * 将临时文件引用或未绑定的聊天引用收口到指定聊天消息ID。
     * <p>
     * 核心逻辑：文件上传时产生的临时引用，在聊天消息发送时需要被"绑定"到具体消息ID上。
     * 此方法负责完成引用类型的转换，并保证幂等（同一消息ID重复绑定不会重复创建记录）。
     * </p>
     * <p>
     * 流程说明：
     * <ol>
     *   <li>校验原引用可被消费（归属、类型、是否已绑定消息）</li>
     *   <li>尝试查找是否已存在该消息ID的绑定引用</li>
     *   <li>不存在则创建新绑定，重复则直接返回已有引用</li>
     *   <li>原引用若与新绑定不同则执行删除（避免同一文件被两个引用持有）</li>
     *   <li>刷新文件元数据引用计数</li>
     * </ol>
     * </p>
     *
     * @param userId           所属用户ID
     * @param businessId       源文件业务引用ID（通常为临时引用）
     * @param messageId        目标聊天消息ID
     * @param chatReferenceType 聊天引用类型
     * @param chatCategory      业务分类
     * @return 绑定后的聊天文件业务引用
     */
    @Override
    public FileBusinessInfo bindChatMessageReference(Long userId,
                                                     Long businessId,
                                                     Long messageId,
                                                     String chatReferenceType,
                                                     String chatCategory) {
        log.info("[聊天文件] 绑定文件引用到消息，userId: {}，businessId: {}，messageId: {}，chatReferenceType: {}",
                userId, businessId, messageId, chatReferenceType);
        ExceptionThrowerCore.throwBusinessIfNull(messageId, ResultErrorCode.ILLEGAL_ARGUMENT, "消息ID不能为空");

        // 校验源引用可被消费（归属、类型、是否已绑定消息）
        FileBusinessInfo sourceReference = requireChatConsumableReference(userId, businessId, chatReferenceType);
        // 校验文件实体存在且正常
        FileInfo fileInfo = requireNormalFile(sourceReference.getFileId());

        // 查找是否已存在该消息ID的绑定引用（幂等关键：同一消息不重复创建）
        FileBusinessInfo chatReference = fileBusinessInfoRepository.findLatestByFileUserReference(
                fileInfo.getId(),
                sourceReference.getUserId(),
                chatReferenceType,
                messageId
        );

        if (chatReference == null) {
            // 无已有引用，创建新的聊天文件业务引用
            log.debug("[聊天文件] 首次绑定，创建新引用，fileId: {}", fileInfo.getId());
            chatReference = new FileBusinessInfo();
            chatReference.setFileId(fileInfo.getId());
            chatReference.setUserId(sourceReference.getUserId());
            chatReference.setReferenceType(chatReferenceType);
            chatReference.setReferenceId(messageId);
            chatReference.setSourceIp(sourceReference.getSourceIp());
            chatReference.setIsPublic(sourceReference.getIsPublic());
            chatReference.setCategory(chatCategory);
            chatReference.setRemark(sourceReference.getRemark());
            try {
                fileBusinessInfoRepository.save(chatReference);
                log.debug("[聊天文件] 新引用创建成功，id: {}", chatReference.getId());
            } catch (DuplicateKeyException ex) {
                // 并发创建导致唯一键冲突，降级为查询已有引用
                log.debug("[聊天文件] 重复键异常，转为查询已有引用，fileId: {}，messageId: {}", fileInfo.getId(), messageId);
                chatReference = fileBusinessInfoRepository.findLatestByFileUserReference(
                        fileInfo.getId(),
                        sourceReference.getUserId(),
                        chatReferenceType,
                        messageId
                );
            }
        } else {
            log.debug("[聊天文件] 引用已存在，复用已有记录，id: {}", chatReference.getId());
        }

        // 若源引用与新绑定不同，则删除源引用（避免同一文件被两个引用持有）
        if (sourceReference.getId() != null && !Objects.equals(sourceReference.getId(), chatReference.getId())) {
            log.debug("[聊天文件] 删除源引用，sourceId: {}", sourceReference.getId());
            fileBusinessInfoRepository.removeById(sourceReference.getId());
        }

        // 刷新文件元数据引用计数，驱动生命周期状态更新
        fileLifecycleService.refreshReferenceMetadata(fileInfo.getId(), Integer.valueOf(1).equals(chatReference.getIsPublic()));
        log.info("[聊天文件] 绑定完成，chatReferenceId: {}，fileId: {}，isPublic: {}",
                chatReference.getId(), fileInfo.getId(), chatReference.getIsPublic());
        return chatReference;
    }


    /**
     * 释放指定业务引用下的全部文件绑定，并同步回刷文件生命周期。
     * <p>
     * 在聊天消息被撤回或删除时调用，负责清理消息与文件的绑定关系。
     * 内部会查询该业务引用下的所有文件引用，统一删除后逐个触发文件生命周期同步。
     * </p>
     *
     * @param referenceType 业务引用类型（如聊天消息类型）
     * @param referenceId   业务引用ID（如消息ID）
     */
    @Override
    public void releaseReferences(String referenceType, Long referenceId) {
        log.info("[聊天文件] 释放文件引用，referenceType: {}，referenceId: {}", referenceType, referenceId);
        // 参数校验，无效参数直接跳过
        if (!StrUtils.hasText(referenceType) || referenceId == null) {
            log.debug("[聊天文件] 参数无效，跳过释放");
            return;
        }

        // 查询该业务引用下的所有文件引用
        List<FileBusinessInfo> references = fileBusinessInfoRepository.listByReferenceTypeAndReferenceId(referenceType, referenceId);
        if (references.isEmpty()) {
            log.debug("[聊天文件] 无引用需要释放");
            return;
        }

        log.debug("[聊天文件] 待释放引用数量: {}", references.size());

        // 收集所有关联的文件ID，用于后续生命周期同步
        Set<Long> fileIds = new LinkedHashSet<>();
        for (FileBusinessInfo reference : references) {
            if (reference.getFileId() != null) {
                fileIds.add(reference.getFileId());
            }
        }

        // 批量删除文件引用
        fileBusinessInfoRepository.removeByIds(references.stream().map(FileBusinessInfo::getId).toList());
        log.info("[聊天文件] 已删除 {} 个文件引用，开始同步文件生命周期", references.size());

        // 逐个触发文件生命周期同步（引用计数刷新 + 状态流转判断）
        fileIds.forEach(fileLifecycleService::syncFileAfterReferenceRemoval);
    }


    /**
     * 按 ID 查询文件信息；文件不存在时返回 null。
     *
     * @param fileId 文件ID
     * @return 文件实体，若不存在则返回 null
     */
    @Override
    public FileInfo getFileInfo(Long fileId) {
        if (fileId == null) {
            return null;
        }
        FileInfo fileInfo = fileInfoRepository.getById(fileId);
        log.debug("[聊天文件] 查询文件信息，fileId: {}，result: {}", fileId, fileInfo != null ? "found" : "not found");
        return fileInfo;
    }

    /**
     * 校验业务引用属于当前用户，且仍可被聊天发送链路消费。
     * <p>
     * 消费条件：
     * <ul>
     *   <li>引用存在</li>
     *   <li>若 userId 不为 null，则引用必须属于该用户</li>
     *   <li>引用类型必须是临时引用（temp）或未绑定消息的聊天引用</li>
     *   <li>聊天引用不能已绑定消息ID（&gt;0）</li>
     * </ul>
     * </p>
     *
     * @param userId           当前用户ID，null表示不校验归属
     * @param businessId       文件业务引用ID
     * @param chatReferenceType 聊天引用类型，用于校验引用来源
     * @return 合法的业务引用
     * @throws BusinessException 引用不存在、无权使用、引用类型不合法
     */
    private FileBusinessInfo requireChatConsumableReference(Long userId, Long businessId, String chatReferenceType) {
        // 查询文件业务引用
        FileBusinessInfo sourceReference = fileBusinessInfoRepository.getById(businessId);
        ExceptionThrowerCore.throwBusinessIfNull(sourceReference, ResultErrorCode.ILLEGAL_ARGUMENT, "文件业务引用不存在");

        // userId 不为 null 时，校验引用归属权（不能发送他人的文件）
        if (userId != null) {
            ExceptionThrowerCore.throwBusinessIf(!Objects.equals(sourceReference.getUserId(), userId), ResultErrorCode.FORBIDDEN, "不能发送他人的文件");
        }

        // 提取引用类型，判断是否为临时引用或聊天引用
        String referenceType = StrUtils.trimToNull(sourceReference.getReferenceType());
        boolean tempReference = Objects.equals(referenceType, FileReferenceTypeEnum.TEMP.getValue());
        boolean chatReference = Objects.equals(referenceType, chatReferenceType);

        // 引用类型必须是临时引用或聊天引用之一，否则不能直接用于聊天消息
        ExceptionThrowerCore.throwBusinessIf(!tempReference && !chatReference, ResultErrorCode.ILLEGAL_ARGUMENT, "当前文件引用不能直接用于聊天消息");

        // 聊天引用不能已绑定消息ID（>0 表示已绑定），否则不允许重复绑定
        ExceptionThrowerCore.throwBusinessIf(chatReference && sourceReference.getReferenceId() != null && sourceReference.getReferenceId() > 0L,
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "当前文件已经绑定到聊天消息");
        return sourceReference;
    }

    /**
     * 校验文件实体存在且仍处于正常状态。
     *
     * @param fileId 文件ID
     * @return 正常状态的文件实体
     * @throws BusinessException 文件不存在或已下线
     */
    private FileInfo requireNormalFile(Long fileId) {
        FileInfo fileInfo = fileInfoRepository.getById(fileId);
        boolean normal = fileInfo != null && Objects.equals(fileInfo.getStatus(), FileStatusEnum.NORMAL.getValue());
        log.debug("[聊天文件] 校验文件状态，fileId: {}，status: {}，normal: {}", fileId, fileInfo != null ? fileInfo.getStatus() : "null", normal);
        ExceptionThrowerCore.throwBusinessIf(fileInfo == null || !normal,
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "文件不存在或不可发送");
        return fileInfo;
    }
}
