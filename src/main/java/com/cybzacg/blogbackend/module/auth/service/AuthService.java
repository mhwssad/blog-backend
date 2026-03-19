package com.cybzacg.blogbackend.module.auth.service;

import com.cybzacg.blogbackend.module.auth.model.AuthEmailCodeRequest;
import com.cybzacg.blogbackend.module.auth.model.AuthEmailLoginRequest;
import com.cybzacg.blogbackend.module.auth.model.AuthLoginRequest;
import com.cybzacg.blogbackend.module.auth.model.AuthMenuInfo;
import com.cybzacg.blogbackend.module.auth.model.AuthRefreshRequest;
import com.cybzacg.blogbackend.module.auth.model.AuthRegisterRequest;
import com.cybzacg.blogbackend.module.auth.model.AuthUserInfo;
import com.cybzacg.blogbackend.module.auth.model.AuthenticationToken;

import java.util.List;

/**
 * 认证服务接口。
 *
 * <p>定义认证相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface AuthService {

    AuthenticationToken login(AuthLoginRequest request, String loginIp);

    AuthenticationToken register(AuthRegisterRequest request, String loginIp);

    void sendEmailLoginCode(AuthEmailCodeRequest request);

    AuthenticationToken emailLogin(AuthEmailLoginRequest request, String loginIp);

    AuthenticationToken refresh(AuthRefreshRequest request);

    void logout(String token);

    AuthUserInfo getCurrentUser();

    List<AuthMenuInfo> getCurrentUserMenus();
}
