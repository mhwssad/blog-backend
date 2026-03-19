package com.cybzacg.blogbackend.module.auth.service;

import com.cybzacg.blogbackend.domain.SysRole;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 系统角色服务接口。
 *
 * <p>定义系统角色相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface SysRoleService extends IService<SysRole> {

    List<String> listRoleCodesByUserId(Long userId);
}
