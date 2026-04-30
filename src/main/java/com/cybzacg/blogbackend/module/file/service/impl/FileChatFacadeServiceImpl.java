package com.cybzacg.blogbackend.module.file.service.impl;

import com.cybzacg.blogbackend.domain.file.FileBusinessInfo;
import com.cybzacg.blogbackend.domain.file.FileInfo;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.enums.file.FileReferenceTypeEnum;
import com.cybzacg.blogbackend.enums.file.FileStatusEnum;
import com.cybzacg.blogbackend.module.file.repository.FileBusinessInfoRepository;
import com.cybzacg.blogbackend.module.file.repository.FileInfoRepository;
import com.cybzacg.blogbackend.module.file.service.FileChatFacadeService;
import com.cybzacg.blogbackend.module.file.service.FileLifecycleService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import com.cybzacg.blogbackend.utils.StrUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 文件域对聊天模块暴露的 facade 实现。
 */
@Service
@RequiredArgsConstructor
public class FileChatFacadeServiceImpl implements FileChatFacadeService {
    private final FileBusinessInfoRepository fileBusinessInfoRepository;
    private final FileInfoRepository fileInfoRepository;
    private final FileLifecycleService fileLifecycleService;

    /**
     * {@inheritDoc}
     */
    @Override
    public FileInfo requireSendableChatFile(Long userId, Long businessId, String chatReferenceType) {
        FileBusinessInfo sourceReference = requireChatConsumableReference(userId, businessId, chatReferenceType);
        return requireNormalFile(sourceReference.getFileId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileBusinessInfo bindChatMessageReference(Long userId,
                                                     Long businessId,
                                                     Long messageId,
                                                     String chatReferenceType,
                                                     String chatCategory) {
        ExceptionThrowerCore.throwBusinessIfNull(messageId, ResultErrorCode.ILLEGAL_ARGUMENT, "消息ID不能为空");
        FileBusinessInfo sourceReference = requireChatConsumableReference(userId, businessId, chatReferenceType);
        FileInfo fileInfo = requireNormalFile(sourceReference.getFileId());
        FileBusinessInfo chatReference = fileBusinessInfoRepository.findLatestByFileUserReference(
                fileInfo.getId(),
                sourceReference.getUserId(),
                chatReferenceType,
                messageId
        );
        if (chatReference == null) {
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
            } catch (DuplicateKeyException ex) {
                chatReference = fileBusinessInfoRepository.findLatestByFileUserReference(
                        fileInfo.getId(),
                        sourceReference.getUserId(),
                        chatReferenceType,
                        messageId
                );
            }
        }
        if (sourceReference.getId() != null && !Objects.equals(sourceReference.getId(), chatReference.getId())) {
            fileBusinessInfoRepository.removeById(sourceReference.getId());
        }
        fileLifecycleService.refreshReferenceMetadata(fileInfo.getId(), Integer.valueOf(1).equals(chatReference.getIsPublic()));
        return chatReference;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void releaseReferences(String referenceType, Long referenceId) {
        if (!StrUtils.hasText(referenceType) || referenceId == null) {
            return;
        }
        List<FileBusinessInfo> references = fileBusinessInfoRepository.listByReferenceTypeAndReferenceId(referenceType, referenceId);
        if (references.isEmpty()) {
            return;
        }
        Set<Long> fileIds = new LinkedHashSet<>();
        for (FileBusinessInfo reference : references) {
            if (reference.getFileId() != null) {
                fileIds.add(reference.getFileId());
            }
        }
        fileBusinessInfoRepository.removeByIds(references.stream().map(FileBusinessInfo::getId).toList());
        fileIds.forEach(fileLifecycleService::syncFileAfterReferenceRemoval);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileInfo getFileInfo(Long fileId) {
        return fileId == null ? null : fileInfoRepository.getById(fileId);
    }

    /**
     * 校验业务引用属于当前用户，且仍可被聊天发送链路消费。
     */
    private FileBusinessInfo requireChatConsumableReference(Long userId, Long businessId, String chatReferenceType) {
        FileBusinessInfo sourceReference = fileBusinessInfoRepository.getById(businessId);
        ExceptionThrowerCore.throwBusinessIfNull(sourceReference, ResultErrorCode.ILLEGAL_ARGUMENT, "文件业务引用不存在");
        if (userId != null) {
            ExceptionThrowerCore.throwBusinessIf(!Objects.equals(sourceReference.getUserId(), userId), ResultErrorCode.FORBIDDEN, "不能发送他人的文件");
        }
        String referenceType = StrUtils.trimToNull(sourceReference.getReferenceType());
        boolean tempReference = Objects.equals(referenceType, FileReferenceTypeEnum.TEMP.getValue());
        boolean chatReference = Objects.equals(referenceType, chatReferenceType);
        ExceptionThrowerCore.throwBusinessIf(!tempReference && !chatReference, ResultErrorCode.ILLEGAL_ARGUMENT, "当前文件引用不能直接用于聊天消息");
        ExceptionThrowerCore.throwBusinessIf(chatReference && sourceReference.getReferenceId() != null && sourceReference.getReferenceId() > 0L,
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "当前文件已经绑定到聊天消息");
        return sourceReference;
    }

    /**
     * 校验文件实体存在且仍处于正常状态。
     */
    private FileInfo requireNormalFile(Long fileId) {
        FileInfo fileInfo = fileInfoRepository.getById(fileId);
        ExceptionThrowerCore.throwBusinessIf(fileInfo == null || !Objects.equals(fileInfo.getStatus(), FileStatusEnum.NORMAL.getValue()),
                ResultErrorCode.ILLEGAL_ARGUMENT,
                "文件不存在或不可发送");
        return fileInfo;
    }
}
