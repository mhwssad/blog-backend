package com.cybzacg.blogbackend.module.ai.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cybzacg.blogbackend.domain.ai.AiChannelAccount;
import com.cybzacg.blogbackend.mapper.ai.AiChannelAccountMapper;
import com.cybzacg.blogbackend.module.ai.repository.AiChannelAccountRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 渠道账号池数据访问实现。
 */
@Repository
public class AiChannelAccountRepositoryImpl
        extends ServiceImpl<AiChannelAccountMapper, AiChannelAccount>
        implements AiChannelAccountRepository {

    @Override
    public List<AiChannelAccount> listEnabledByChannelId(Long channelConfigId) {
        return list(new LambdaQueryWrapper<AiChannelAccount>()
                .eq(AiChannelAccount::getChannelConfigId, channelConfigId)
                .eq(AiChannelAccount::getStatus, 1)
                .orderByDesc(AiChannelAccount::getWeight)
                .orderByAsc(AiChannelAccount::getId));
    }

    @Override
    public boolean resetErrors(Long id) {
        return update(new LambdaUpdateWrapper<AiChannelAccount>()
                .eq(AiChannelAccount::getId, id)
                .set(AiChannelAccount::getConsecutiveErrors, 0)
                .set(AiChannelAccount::getLastErrorAt, null)
                .set(AiChannelAccount::getLastErrorMessage, null)
                .setSql("total_call_count = total_call_count + 1")
                .set(AiChannelAccount::getLastUsedAt, LocalDateTime.now()));
    }

    @Override
    public boolean incrementErrors(Long id, String errorMessage) {
        AiChannelAccount account = getById(id);
        if (account == null) {
            return false;
        }

        int newErrors = (account.getConsecutiveErrors() != null ? account.getConsecutiveErrors() : 0) + 1;
        int maxErrors = account.getMaxConsecutiveErrors() != null ? account.getMaxConsecutiveErrors() : 5;
        boolean shouldDisable = newErrors >= maxErrors;

        LambdaUpdateWrapper<AiChannelAccount> wrapper = new LambdaUpdateWrapper<AiChannelAccount>()
                .eq(AiChannelAccount::getId, id)
                .set(AiChannelAccount::getConsecutiveErrors, newErrors)
                .set(AiChannelAccount::getLastErrorAt, LocalDateTime.now())
                .set(AiChannelAccount::getLastErrorMessage, truncateMessage(errorMessage));

        if (shouldDisable) {
            LocalDateTime now = LocalDateTime.now();
            wrapper.set(AiChannelAccount::getStatus, 0)
                    .set(AiChannelAccount::getDisabledAt, now)
                    .set(AiChannelAccount::getAutoRecoverAt, now.plusMinutes(10));
        }

        return update(wrapper);
    }

    @Override
    public int recoverDisabledAccounts() {
        List<AiChannelAccount> accounts = list(new LambdaQueryWrapper<AiChannelAccount>()
                .eq(AiChannelAccount::getStatus, 0)
                .le(AiChannelAccount::getAutoRecoverAt, LocalDateTime.now()));

        if (accounts.isEmpty()) {
            return 0;
        }

        for (AiChannelAccount account : accounts) {
            update(new LambdaUpdateWrapper<AiChannelAccount>()
                    .eq(AiChannelAccount::getId, account.getId())
                    .set(AiChannelAccount::getStatus, 1)
                    .set(AiChannelAccount::getConsecutiveErrors, 0)
                    .set(AiChannelAccount::getDisabledAt, null)
                    .set(AiChannelAccount::getAutoRecoverAt, null)
                    .set(AiChannelAccount::getLastErrorAt, null)
                    .set(AiChannelAccount::getLastErrorMessage, null));
        }

        return accounts.size();
    }

    private String truncateMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 512 ? message.substring(0, 512) : message;
    }
}
