package com.cybzacg.blogbackend.module.auth.account.service;

import com.cybzacg.blogbackend.module.auth.account.model.admin.AccountTakeoverResponse;
import org.springframework.security.core.Authentication;

/**
 * 账号接管服务接口。
 */
public interface AccountTakeoverService {
    /**
     * 超级管理员接管目标用户账号。
     */
    AccountTakeoverResponse takeover(Long operatorId, Long targetUserId, String mfaTicket, String ip, String ua);

    /**
     * 解析接管令牌，构建目标用户的认证对象。
     */
    Authentication resolveTakeover(String takeoverToken);
}
