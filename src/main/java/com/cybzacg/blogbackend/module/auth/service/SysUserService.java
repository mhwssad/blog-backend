package com.cybzacg.blogbackend.module.auth.service;

import com.cybzacg.blogbackend.domain.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 系统用户服务接口。
 *
 * <p>定义系统用户相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface SysUserService extends IService<SysUser> {

    SysUser getByUsername(String username);

    SysUser getByEmail(String email);

    boolean updateLoginInfo(Long userId, String ip);
}
