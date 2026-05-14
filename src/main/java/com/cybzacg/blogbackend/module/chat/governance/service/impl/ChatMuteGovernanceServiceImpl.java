package com.cybzacg.blogbackend.module.chat.governance.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.dto.domain.auth.SysUser;
import com.cybzacg.blogbackend.dto.domain.chat.ChatConversation;
import com.cybzacg.blogbackend.dto.domain.chat.ChatUserMuteRecord;
import com.cybzacg.blogbackend.dto.repository.auth.account.SysUserRepository;
import com.cybzacg.blogbackend.dto.repository.chat.conversation.ChatConversationRepository;
import com.cybzacg.blogbackend.dto.repository.chat.governance.ChatUserMuteRecordRepository;
import com.cybzacg.blogbackend.enums.chat.ChatMuteRecordStatusEnum;
import com.cybzacg.blogbackend.enums.chat.ChatMuteScopeEnum;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.chat.governance.convert.ChatMuteModelConvert;
import com.cybzacg.blogbackend.module.chat.governance.model.admin.ChatMuteCreateRequest;
import com.cybzacg.blogbackend.module.chat.governance.model.admin.ChatMuteRecordVO;
import com.cybzacg.blogbackend.module.chat.governance.service.ChatMuteGovernanceService;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 统一禁言治理服务实现。
 */
@Service
@RequiredArgsConstructor
public class ChatMuteGovernanceServiceImpl implements ChatMuteGovernanceService {

    private final ChatUserMuteRecordRepository muteRecordRepository;
    private final SysUserRepository sysUserRepository;
    private final ChatConversationRepository conversationRepository;
    private final ChatMuteModelConvert muteModelConvert;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMuteRecordVO createMute(ChatMuteCreateRequest request, Long operatorId) {
        validateScope(request.getScope());
        validateConversationRequirement(request.getScope(), request.getConversationId());
        requireUser(request.getUserId());

        ChatUserMuteRecord record = muteModelConvert.toEntity(request, operatorId);
        muteRecordRepository.save(record);
        return enrichRecordVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseMute(Long recordId, Long operatorId) {
        ChatUserMuteRecord record = requireRecord(recordId);
        ExceptionThrowerCore.throwBusinessIf(
                Objects.equals(record.getStatus(), ChatMuteRecordStatusEnum.RELEASED.getValue()),
                ResultErrorCode.CHAT_MUTE_ALREADY_RELEASED);

        record.setStatus(ChatMuteRecordStatusEnum.RELEASED.getValue());
        record.setReleasedBy(operatorId);
        record.setReleasedAt(LocalDateTime.now());
        muteRecordRepository.updateById(record);
    }

    @Override
    public Page<ChatMuteRecordVO> pageMutes(Long userId, String scope, Integer status, Long current, Long size) {
        Page<ChatUserMuteRecord> page = new Page<>(current, size);
        Page<ChatUserMuteRecord> result = muteRecordRepository.pageByAdminConditions(userId, scope, status, page);

        List<ChatMuteRecordVO> voList = result.getRecords().stream()
                .map(this::enrichRecordVO)
                .collect(Collectors.toList());
        enrichBatchUsernames(voList);

        Page<ChatMuteRecordVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public boolean isUserMuted(Long userId, Long conversationId, String scope) {
        // 检查全站禁言
        if (!ChatMuteScopeEnum.GLOBAL.code.equals(scope)) {
            List<ChatUserMuteRecord> globalRecords = muteRecordRepository.findActiveByUserIdAndScope(
                    userId, ChatMuteScopeEnum.GLOBAL.code);
            if (hasActiveMute(globalRecords)) {
                return true;
            }
        }

        // 检查指定 scope 禁言
        if (ChatMuteScopeEnum.GLOBAL.code.equals(scope)) {
            List<ChatUserMuteRecord> records = muteRecordRepository.findActiveByUserIdAndScope(userId, scope);
            return hasActiveMute(records);
        }

        // 非 global scope 需要同时检查 scope 级别和 conversationId 级别
        List<ChatUserMuteRecord> scopeRecords = muteRecordRepository.findActiveByUserIdAndScope(userId, scope);
        if (hasActiveMute(scopeRecords)) {
            return true;
        }

        if (conversationId != null) {
            List<ChatUserMuteRecord> convRecords = muteRecordRepository.findActiveByUserIdAndConversationId(
                    userId, conversationId);
            return hasActiveMute(convRecords);
        }

        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMuteRecordVO createMuteFromReport(Long userId, String scope, Long conversationId,
                                                  String reason, Long reportId, Long operatorId,
                                                  LocalDateTime muteUntil) {
        validateScope(scope);

        ChatUserMuteRecord record = new ChatUserMuteRecord();
        record.setUserId(userId);
        record.setScope(scope);
        record.setConversationId(conversationId);
        record.setMuteUntil(muteUntil);
        record.setStatus(ChatMuteRecordStatusEnum.ACTIVE.getValue());
        record.setReason(reason);
        record.setSourceType("report");
        record.setReportId(reportId);
        record.setOperatorId(operatorId);
        muteRecordRepository.save(record);
        return enrichRecordVO(record);
    }

    // ==================== 私有辅助方法 ====================

    private void validateScope(String scope) {
        // Validation handled by JSR-303 annotations on DTO
    }

    private void validateConversationRequirement(String scope, Long conversationId) {
        // Validation handled by JSR-303 annotations on DTO
    }

    private SysUser requireUser(Long userId) {
        SysUser user = sysUserRepository.getById(userId);
        ExceptionThrowerCore.throwBusinessIfNull(user, ResultErrorCode.USER_NOT_FOUND);
        return user;
    }

    private ChatUserMuteRecord requireRecord(Long recordId) {
        ChatUserMuteRecord record = muteRecordRepository.getById(recordId);
        ExceptionThrowerCore.throwBusinessIfNull(record, ResultErrorCode.CHAT_MUTE_RECORD_NOT_FOUND);
        return record;
    }

    private boolean hasActiveMute(List<ChatUserMuteRecord> records) {
        if (records == null || records.isEmpty()) return false;
        LocalDateTime now = LocalDateTime.now();
        return records.stream().anyMatch(r ->
                Objects.equals(r.getStatus(), ChatMuteRecordStatusEnum.ACTIVE.getValue())
                        && (r.getMuteUntil() == null || r.getMuteUntil().isAfter(now))
        );
    }

    private ChatMuteRecordVO enrichRecordVO(ChatUserMuteRecord record) {
        ChatMuteRecordVO vo = muteModelConvert.toRecordVO(record);

        if (record.getUserId() != null) {
            SysUser user = sysUserRepository.getById(record.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
            }
        }
        if (record.getOperatorId() != null) {
            SysUser operator = sysUserRepository.getById(record.getOperatorId());
            if (operator != null) {
                vo.setOperatorUsername(operator.getUsername());
            }
        }
        if (record.getConversationId() != null) {
            ChatConversation conversation = conversationRepository.getById(record.getConversationId());
            if (conversation != null) {
                vo.setConversationName(conversation.getName());
            }
        }
        return vo;
    }

    private void enrichBatchUsernames(List<ChatMuteRecordVO> voList) {
        if (voList == null || voList.isEmpty()) return;

        List<Long> userIds = voList.stream()
                .map(ChatMuteRecordVO::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> operatorIds = voList.stream()
                .map(ChatMuteRecordVO::getOperatorId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        userIds.addAll(operatorIds);

        if (userIds.isEmpty()) return;

        Map<Long, SysUser> userMap = sysUserRepository.listByIds(userIds.stream().distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        for (ChatMuteRecordVO vo : voList) {
            SysUser user = userMap.get(vo.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
            }
            SysUser operator = userMap.get(vo.getOperatorId());
            if (operator != null) {
                vo.setOperatorUsername(operator.getUsername());
            }
        }
    }
}
