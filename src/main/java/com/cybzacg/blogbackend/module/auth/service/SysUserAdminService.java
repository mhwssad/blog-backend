package com.cybzacg.blogbackend.module.auth.service;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysUserAdminVO;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysUserPageQuery;
import com.cybzacg.blogbackend.module.auth.rbac.model.admin.SysUserSaveRequest;

import java.util.List;

/**
 * 系统用户后台管理服务接口。
 *
 * <p>定义系统用户后台管理相关业务能力，对上层控制器提供稳定的业务契约。
 */
public interface SysUserAdminService {
    PageResult<SysUserAdminVO> pageUsers(SysUserPageQuery query);

    SysUserAdminVO getUser(Long id);

    SysUserAdminVO createUser(SysUserSaveRequest request);

    SysUserAdminVO updateUser(Long id, SysUserSaveRequest request);

    void updateStatus(Long id, Integer status);

    void resetPassword(Long id, String password);

    void deleteUser(Long id);

    List<Long> listRoleIds(Long userId);

    void assignRoles(Long userId, List<Long> roleIds);

    void banUser(Long operatorId, Long targetId, String mfaTicket, String ip, String ua);

    void banUserByReport(Long operatorId, Long targetId, String reason, String ip, String ua);

    void unbanUser(Long operatorId, Long targetId, String mfaTicket, String ip, String ua);

    void adjustLevel(Long operatorId, Long targetId, Integer newLevel, String mfaTicket, String ip, String ua);

    void adjustExperience(Long operatorId, Long targetId, Integer newExperience, String mfaTicket, String ip, String ua);

    void assignRolesWithAudit(Long operatorId, Long targetId, List<Long> roleIds, String mfaTicket, String ip, String ua);
}
