package com.cybzacg.blogbackend.module.auth.service;

import com.cybzacg.blogbackend.module.auth.model.AuthUserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * 认证用户Details服务接口。
 *
 * <p>定义认证用户Details相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface AuthUserDetailsService extends UserDetailsService {

    AuthUserDetails loadAuthUserByUsername(String username);
}
