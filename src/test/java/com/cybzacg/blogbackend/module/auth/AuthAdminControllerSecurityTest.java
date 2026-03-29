package com.cybzacg.blogbackend.module.auth;

import com.cybzacg.blogbackend.core.security.SecurityPermissionChecker;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.auth.controller.SysConfigAdminController;
import com.cybzacg.blogbackend.module.auth.controller.SysLogAdminController;
import com.cybzacg.blogbackend.module.auth.controller.SysMenuAdminController;
import com.cybzacg.blogbackend.module.auth.controller.SysNoticeAdminController;
import com.cybzacg.blogbackend.module.auth.controller.SysRoleAdminController;
import com.cybzacg.blogbackend.module.auth.controller.SysUserAdminController;
import com.cybzacg.blogbackend.module.auth.model.admin.SysConfigAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysLogAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysMenuAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysNoticeAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysRoleAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysUserAdminVO;
import com.cybzacg.blogbackend.module.auth.service.SysConfigAdminService;
import com.cybzacg.blogbackend.module.auth.service.SysLogAdminService;
import com.cybzacg.blogbackend.module.auth.service.SysMenuAdminService;
import com.cybzacg.blogbackend.module.auth.service.SysNoticeAdminService;
import com.cybzacg.blogbackend.module.auth.service.SysRoleAdminService;
import com.cybzacg.blogbackend.module.auth.service.SysUserAdminService;
import com.cybzacg.blogbackend.utils.HttpServletResponseUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = AuthAdminControllerSecurityTest.TestConfig.class)
class AuthAdminControllerSecurityTest {
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private SysRoleAdminService sysRoleAdminService;
    @Autowired
    private SysUserAdminService sysUserAdminService;
    @Autowired
    private SysNoticeAdminService sysNoticeAdminService;
    @Autowired
    private SysConfigAdminService sysConfigAdminService;
    @Autowired
    private SysLogAdminService sysLogAdminService;
    @Autowired
    private SysMenuAdminService sysMenuAdminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Mockito.reset(sysRoleAdminService, sysUserAdminService, sysNoticeAdminService,
                sysConfigAdminService, sysLogAdminService, sysMenuAdminService);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(authorities = "sys:role:assign-menu")
    void assignMenusShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(put("/api/sys/roles/7/menus")
                        .contentType(APPLICATION_JSON)
                        .content("{\"menuIds\":[1,2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysRoleAdminService).assignMenus(7L, java.util.List.of(1L, 2L));
    }

    @Test
    @WithMockUser(authorities = "sys:role:query")
    void assignMenusShouldRejectUserWithoutAssignPermission() throws Exception {
        mockMvc.perform(put("/api/sys/roles/7/menus")
                        .contentType(APPLICATION_JSON)
                        .content("{\"menuIds\":[1,2]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysRoleAdminService, never()).assignMenus(7L, java.util.List.of(1L, 2L));
    }

    @Test
    @WithMockUser(authorities = "sys:role:query")
    void pageRolesShouldAllowAuthorizedUser() throws Exception {
        when(sysRoleAdminService.pageRoles(org.mockito.ArgumentMatchers.any()))
                .thenReturn(PageResult.<SysRoleAdminVO>builder()
                        .total(0L)
                        .current(1L)
                        .size(10L)
                        .records(java.util.List.of())
                        .build());

        mockMvc.perform(get("/api/sys/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @WithMockUser(authorities = "sys:role:update")
    void pageRolesShouldRejectUserWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/sys/roles"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysRoleAdminService, never()).pageRoles(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:role:query")
    void getRoleShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(get("/api/sys/roles/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysRoleAdminService).getRole(7L);
    }

    @Test
    @WithMockUser(authorities = "sys:role:update")
    void getRoleShouldRejectUserWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/sys/roles/7"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysRoleAdminService, never()).getRole(7L);
    }

    @Test
    @WithMockUser(authorities = "sys:role:create")
    void createRoleShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(post("/api/sys/roles")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Admin\",\"code\":\"admin\",\"sort\":1,\"status\":1,\"dataScope\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysRoleAdminService).createRole(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:role:query")
    void createRoleShouldRejectUserWithoutCreatePermission() throws Exception {
        mockMvc.perform(post("/api/sys/roles")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Admin\",\"code\":\"admin\",\"sort\":1,\"status\":1,\"dataScope\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysRoleAdminService, never()).createRole(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:role:update")
    void updateRoleShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(put("/api/sys/roles/7")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Admin\",\"code\":\"admin\",\"sort\":2,\"status\":1,\"dataScope\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysRoleAdminService).updateRole(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:role:query")
    void updateRoleShouldRejectUserWithoutUpdatePermission() throws Exception {
        mockMvc.perform(put("/api/sys/roles/7")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Admin\",\"code\":\"admin\",\"sort\":2,\"status\":1,\"dataScope\":2}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysRoleAdminService, never()).updateRole(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:role:delete")
    void deleteRoleShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(delete("/api/sys/roles/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysRoleAdminService).deleteRole(7L);
    }

    @Test
    @WithMockUser(authorities = "sys:role:query")
    void deleteRoleShouldRejectUserWithoutDeletePermission() throws Exception {
        mockMvc.perform(delete("/api/sys/roles/7"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysRoleAdminService, never()).deleteRole(7L);
    }

    @Test
    @WithMockUser(authorities = "sys:role:query")
    void listMenuIdsShouldAllowAuthorizedUser() throws Exception {
        when(sysRoleAdminService.listMenuIds(7L))
                .thenReturn(java.util.List.of(1L, 2L));

        mockMvc.perform(get("/api/sys/roles/7/menus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data[0]").value(1))
                .andExpect(jsonPath("$.data[1]").value(2));
    }

    @Test
    @WithMockUser(authorities = "sys:role:update")
    void listMenuIdsShouldRejectUserWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/sys/roles/7/menus"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysRoleAdminService, never()).listMenuIds(7L);
    }

    @Test
    @WithMockUser(authorities = "sys:role:update")
    void updateRoleStatusShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(put("/api/sys/roles/7/status")
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysRoleAdminService).updateStatus(7L, 1);
    }

    @Test
    @WithMockUser(authorities = "sys:role:query")
    void updateRoleStatusShouldRejectUserWithoutUpdatePermission() throws Exception {
        mockMvc.perform(put("/api/sys/roles/7/status")
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysRoleAdminService, never()).updateStatus(7L, 1);
    }

    @Test
    @WithMockUser(authorities = "sys:user:assign-role")
    void assignRolesShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(put("/api/sys/users/9/roles")
                        .contentType(APPLICATION_JSON)
                        .content("{\"roleIds\":[3,4]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysUserAdminService).assignRoles(9L, java.util.List.of(3L, 4L));
    }

    @Test
    @WithMockUser(authorities = "sys:user:query")
    void assignRolesShouldRejectUserWithoutAssignPermission() throws Exception {
        mockMvc.perform(put("/api/sys/users/9/roles")
                        .contentType(APPLICATION_JSON)
                        .content("{\"roleIds\":[3,4]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysUserAdminService, never()).assignRoles(9L, java.util.List.of(3L, 4L));
    }

    @Test
    @WithMockUser(authorities = "sys:user:query")
    void pageUsersShouldAllowAuthorizedUser() throws Exception {
        when(sysUserAdminService.pageUsers(org.mockito.ArgumentMatchers.any()))
                .thenReturn(PageResult.<SysUserAdminVO>builder()
                        .total(0L)
                        .current(1L)
                        .size(10L)
                        .records(java.util.List.of())
                        .build());

        mockMvc.perform(get("/api/sys/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @WithMockUser(authorities = "sys:user:update")
    void pageUsersShouldRejectUserWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/sys/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysUserAdminService, never()).pageUsers(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:user:query")
    void getUserShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(get("/api/sys/users/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysUserAdminService).getUser(9L);
    }

    @Test
    @WithMockUser(authorities = "sys:user:update")
    void getUserShouldRejectUserWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/sys/users/9"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysUserAdminService, never()).getUser(9L);
    }

    @Test
    @WithMockUser(authorities = "sys:user:create")
    void createUserShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(post("/api/sys/users")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"tester\",\"password\":\"secret\",\"email\":\"tester@example.com\",\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysUserAdminService).createUser(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:user:query")
    void createUserShouldRejectUserWithoutCreatePermission() throws Exception {
        mockMvc.perform(post("/api/sys/users")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"tester\",\"password\":\"secret\",\"email\":\"tester@example.com\",\"status\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysUserAdminService, never()).createUser(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:user:update")
    void updateUserShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(put("/api/sys/users/9")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"tester\",\"email\":\"tester@example.com\",\"status\":1,\"nickname\":\"Tester\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysUserAdminService).updateUser(org.mockito.ArgumentMatchers.eq(9L),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:user:query")
    void updateUserShouldRejectUserWithoutUpdatePermission() throws Exception {
        mockMvc.perform(put("/api/sys/users/9")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"tester\",\"email\":\"tester@example.com\",\"status\":1,\"nickname\":\"Tester\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysUserAdminService, never()).updateUser(org.mockito.ArgumentMatchers.eq(9L),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:user:delete")
    void deleteUserShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(delete("/api/sys/users/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysUserAdminService).deleteUser(9L);
    }

    @Test
    @WithMockUser(authorities = "sys:user:query")
    void deleteUserShouldRejectUserWithoutDeletePermission() throws Exception {
        mockMvc.perform(delete("/api/sys/users/9"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysUserAdminService, never()).deleteUser(9L);
    }

    @Test
    @WithMockUser(authorities = "sys:user:query")
    void listRoleIdsShouldAllowAuthorizedUser() throws Exception {
        when(sysUserAdminService.listRoleIds(9L))
                .thenReturn(java.util.List.of(3L, 4L));

        mockMvc.perform(get("/api/sys/users/9/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data[0]").value(3))
                .andExpect(jsonPath("$.data[1]").value(4));
    }

    @Test
    @WithMockUser(authorities = "sys:user:update")
    void listRoleIdsShouldRejectUserWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/sys/users/9/roles"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysUserAdminService, never()).listRoleIds(9L);
    }

    @Test
    @WithMockUser(authorities = "sys:user:update")
    void updateUserStatusShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(put("/api/sys/users/9/status")
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysUserAdminService).updateStatus(9L, 0);
    }

    @Test
    @WithMockUser(authorities = "sys:user:query")
    void updateUserStatusShouldRejectUserWithoutUpdatePermission() throws Exception {
        mockMvc.perform(put("/api/sys/users/9/status")
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":0}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysUserAdminService, never()).updateStatus(9L, 0);
    }

    @Test
    @WithMockUser(authorities = "sys:user:reset-password")
    void resetPasswordShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(put("/api/sys/users/9/password/reset")
                        .contentType(APPLICATION_JSON)
                        .content("{\"password\":\"new-pass-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysUserAdminService).resetPassword(9L, "new-pass-123");
    }

    @Test
    @WithMockUser(authorities = "sys:user:query")
    void resetPasswordShouldRejectUserWithoutResetPermission() throws Exception {
        mockMvc.perform(put("/api/sys/users/9/password/reset")
                        .contentType(APPLICATION_JSON)
                        .content("{\"password\":\"new-pass-123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysUserAdminService, never()).resetPassword(9L, "new-pass-123");
    }

    @Test
    @WithMockUser(authorities = "sys:notice:query")
    void pageNoticesShouldAllowAuthorizedUser() throws Exception {
        when(sysNoticeAdminService.pageNotices(org.mockito.ArgumentMatchers.any()))
                .thenReturn(PageResult.<SysNoticeAdminVO>builder()
                        .total(0L)
                        .current(1L)
                        .size(10L)
                        .records(java.util.List.of())
                        .build());

        mockMvc.perform(get("/api/sys/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @WithMockUser(authorities = "sys:notice:update")
    void pageNoticesShouldRejectUserWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/sys/notices"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysNoticeAdminService, never()).pageNotices(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:notice:query")
    void getNoticeShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(get("/api/sys/notices/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysNoticeAdminService).getNotice(11L);
    }

    @Test
    @WithMockUser(authorities = "sys:notice:update")
    void getNoticeShouldRejectUserWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/sys/notices/11"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysNoticeAdminService, never()).getNotice(11L);
    }

    @Test
    @WithMockUser(authorities = "sys:notice:create")
    void createNoticeShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(post("/api/sys/notices")
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"System Upgrade\",\"content\":\"Tonight 10 PM\",\"type\":1,\"level\":\"INFO\",\"targetType\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysNoticeAdminService).createNotice(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:notice:query")
    void createNoticeShouldRejectUserWithoutCreatePermission() throws Exception {
        mockMvc.perform(post("/api/sys/notices")
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"System Upgrade\",\"content\":\"Tonight 10 PM\",\"type\":1,\"level\":\"INFO\",\"targetType\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysNoticeAdminService, never()).createNotice(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:notice:update")
    void updateNoticeShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(put("/api/sys/notices/11")
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"System Upgrade\",\"content\":\"Tonight 11 PM\",\"type\":1,\"level\":\"WARN\",\"targetType\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysNoticeAdminService).updateNotice(org.mockito.ArgumentMatchers.eq(11L),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:notice:query")
    void updateNoticeShouldRejectUserWithoutUpdatePermission() throws Exception {
        mockMvc.perform(put("/api/sys/notices/11")
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"System Upgrade\",\"content\":\"Tonight 11 PM\",\"type\":1,\"level\":\"WARN\",\"targetType\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysNoticeAdminService, never()).updateNotice(org.mockito.ArgumentMatchers.eq(11L),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:notice:publish")
    void publishNoticeShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(post("/api/sys/notices/11/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysNoticeAdminService).publishNotice(11L);
    }

    @Test
    @WithMockUser(authorities = "sys:notice:query")
    void publishNoticeShouldRejectUserWithoutPublishPermission() throws Exception {
        mockMvc.perform(post("/api/sys/notices/11/publish"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysNoticeAdminService, never()).publishNotice(11L);
    }

    @Test
    @WithMockUser(authorities = "sys:notice:revoke")
    void revokeNoticeShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(post("/api/sys/notices/11/revoke"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysNoticeAdminService).revokeNotice(11L);
    }

    @Test
    @WithMockUser(authorities = "sys:notice:query")
    void revokeNoticeShouldRejectUserWithoutRevokePermission() throws Exception {
        mockMvc.perform(post("/api/sys/notices/11/revoke"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysNoticeAdminService, never()).revokeNotice(11L);
    }

    @Test
    @WithMockUser(authorities = "sys:notice:delete")
    void deleteNoticeShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(delete("/api/sys/notices/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysNoticeAdminService).deleteNotice(11L);
    }

    @Test
    @WithMockUser(authorities = "sys:notice:query")
    void deleteNoticeShouldRejectUserWithoutDeletePermission() throws Exception {
        mockMvc.perform(delete("/api/sys/notices/11"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysNoticeAdminService, never()).deleteNotice(11L);
    }

    @Test
    @WithMockUser(authorities = "sys:config:query")
    void pageConfigsShouldAllowAuthorizedUser() throws Exception {
        when(sysConfigAdminService.pageConfigs(org.mockito.ArgumentMatchers.any()))
                .thenReturn(PageResult.<SysConfigAdminVO>builder()
                        .total(0L)
                        .current(1L)
                        .size(10L)
                        .records(java.util.List.of())
                        .build());

        mockMvc.perform(get("/api/sys/configs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @WithMockUser(authorities = "sys:config:update")
    void pageConfigsShouldRejectUserWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/sys/configs"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysConfigAdminService, never()).pageConfigs(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:config:query")
    void getConfigShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(get("/api/sys/configs/13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysConfigAdminService).getConfig(13L);
    }

    @Test
    @WithMockUser(authorities = "sys:config:update")
    void getConfigShouldRejectUserWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/sys/configs/13"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysConfigAdminService, never()).getConfig(13L);
    }

    @Test
    @WithMockUser(authorities = "sys:config:create")
    void createConfigShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(post("/api/sys/configs")
                        .contentType(APPLICATION_JSON)
                        .content("{\"configName\":\"Site Name\",\"configKey\":\"site.name\",\"configValue\":\"Blog\",\"remark\":\"global\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysConfigAdminService).createConfig(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:config:query")
    void createConfigShouldRejectUserWithoutCreatePermission() throws Exception {
        mockMvc.perform(post("/api/sys/configs")
                        .contentType(APPLICATION_JSON)
                        .content("{\"configName\":\"Site Name\",\"configKey\":\"site.name\",\"configValue\":\"Blog\",\"remark\":\"global\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysConfigAdminService, never()).createConfig(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:config:update")
    void updateConfigShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(put("/api/sys/configs/13")
                        .contentType(APPLICATION_JSON)
                        .content("{\"configName\":\"Site Name\",\"configKey\":\"site.name\",\"configValue\":\"Blog V2\",\"remark\":\"updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysConfigAdminService).updateConfig(org.mockito.ArgumentMatchers.eq(13L),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:config:query")
    void updateConfigShouldRejectUserWithoutUpdatePermission() throws Exception {
        mockMvc.perform(put("/api/sys/configs/13")
                        .contentType(APPLICATION_JSON)
                        .content("{\"configName\":\"Site Name\",\"configKey\":\"site.name\",\"configValue\":\"Blog V2\",\"remark\":\"updated\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysConfigAdminService, never()).updateConfig(org.mockito.ArgumentMatchers.eq(13L),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:config:delete")
    void deleteConfigShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(delete("/api/sys/configs/13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysConfigAdminService).deleteConfig(13L);
    }

    @Test
    @WithMockUser(authorities = "sys:config:query")
    void deleteConfigShouldRejectUserWithoutDeletePermission() throws Exception {
        mockMvc.perform(delete("/api/sys/configs/13"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysConfigAdminService, never()).deleteConfig(13L);
    }

    @Test
    @WithMockUser(authorities = "sys:config:query")
    void getValueByKeyShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(get("/api/sys/configs/key/site.name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysConfigAdminService).getValueByKey("site.name");
    }

    @Test
    @WithMockUser(authorities = "sys:config:update")
    void getValueByKeyShouldRejectUserWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/sys/configs/key/site.name"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysConfigAdminService, never()).getValueByKey("site.name");
    }

    @Test
    @WithMockUser(authorities = "sys:log:query")
    void pageLogsShouldAllowAuthorizedUser() throws Exception {
        when(sysLogAdminService.pageLogs(org.mockito.ArgumentMatchers.any()))
                .thenReturn(PageResult.<SysLogAdminVO>builder()
                        .total(0L)
                        .current(1L)
                        .size(10L)
                        .records(java.util.List.of())
                        .build());

        mockMvc.perform(get("/api/sys/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @WithMockUser(authorities = "sys:log:delete")
    void pageLogsShouldRejectUserWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/sys/logs"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysLogAdminService, never()).pageLogs(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:log:clean")
    void cleanLogsShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(post("/api/sys/logs/clean")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data").value(0));

        verify(sysLogAdminService).cleanLogs(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:log:query")
    void cleanLogsShouldRejectUserWithoutCleanPermission() throws Exception {
        mockMvc.perform(post("/api/sys/logs/clean")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysLogAdminService, never()).cleanLogs(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:log:delete")
    void deleteLogShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(delete("/api/sys/logs/21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysLogAdminService).deleteLog(21L);
    }

    @Test
    @WithMockUser(authorities = "sys:log:query")
    void deleteLogShouldRejectUserWithoutDeletePermission() throws Exception {
        mockMvc.perform(delete("/api/sys/logs/21"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysLogAdminService, never()).deleteLog(21L);
    }

    @Test
    @WithMockUser(authorities = "sys:log:query")
    void getLogShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(get("/api/sys/logs/21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysLogAdminService).getLog(21L);
    }

    @Test
    @WithMockUser(authorities = "sys:log:delete")
    void getLogShouldRejectUserWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/sys/logs/21"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysLogAdminService, never()).getLog(21L);
    }

    @Test
    @WithMockUser(authorities = "sys:menu:query")
    void listMenuTreeShouldAllowAuthorizedUser() throws Exception {
        when(sysMenuAdminService.listMenuTree())
                .thenReturn(java.util.List.<SysMenuAdminVO>of());

        mockMvc.perform(get("/api/sys/menus/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(authorities = "sys:menu:update")
    void listMenuTreeShouldRejectUserWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/sys/menus/tree"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysMenuAdminService, never()).listMenuTree();
    }

    @Test
    @WithMockUser(authorities = "sys:menu:query")
    void getMenuShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(get("/api/sys/menus/17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysMenuAdminService).getMenu(17L);
    }

    @Test
    @WithMockUser(authorities = "sys:menu:update")
    void getMenuShouldRejectUserWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/sys/menus/17"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysMenuAdminService, never()).getMenu(17L);
    }

    @Test
    @WithMockUser(authorities = "sys:menu:create")
    void createMenuShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(post("/api/sys/menus")
                        .contentType(APPLICATION_JSON)
                        .content("{\"parentId\":0,\"name\":\"Dashboard\",\"type\":\"MENU\",\"routePath\":\"/dashboard\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysMenuAdminService).createMenu(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:menu:query")
    void createMenuShouldRejectUserWithoutCreatePermission() throws Exception {
        mockMvc.perform(post("/api/sys/menus")
                        .contentType(APPLICATION_JSON)
                        .content("{\"parentId\":0,\"name\":\"Dashboard\",\"type\":\"MENU\",\"routePath\":\"/dashboard\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysMenuAdminService, never()).createMenu(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:menu:update")
    void updateMenuShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(put("/api/sys/menus/17")
                        .contentType(APPLICATION_JSON)
                        .content("{\"parentId\":0,\"name\":\"Dashboard\",\"type\":\"MENU\",\"routePath\":\"/dashboard-home\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysMenuAdminService).updateMenu(org.mockito.ArgumentMatchers.eq(17L),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:menu:query")
    void updateMenuShouldRejectUserWithoutUpdatePermission() throws Exception {
        mockMvc.perform(put("/api/sys/menus/17")
                        .contentType(APPLICATION_JSON)
                        .content("{\"parentId\":0,\"name\":\"Dashboard\",\"type\":\"MENU\",\"routePath\":\"/dashboard-home\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysMenuAdminService, never()).updateMenu(org.mockito.ArgumentMatchers.eq(17L),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "sys:menu:delete")
    void deleteMenuShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(delete("/api/sys/menus/17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(sysMenuAdminService).deleteMenu(17L);
    }

    @Test
    @WithMockUser(authorities = "sys:menu:query")
    void deleteMenuShouldRejectUserWithoutDeletePermission() throws Exception {
        mockMvc.perform(delete("/api/sys/menus/17"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(sysMenuAdminService, never()).deleteMenu(17L);
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        SysRoleAdminService sysRoleAdminService() {
            return mock(SysRoleAdminService.class);
        }

        @Bean
        SysUserAdminService sysUserAdminService() {
            return mock(SysUserAdminService.class);
        }

        @Bean
        SysNoticeAdminService sysNoticeAdminService() {
            return mock(SysNoticeAdminService.class);
        }

        @Bean
        SysConfigAdminService sysConfigAdminService() {
            return mock(SysConfigAdminService.class);
        }

        @Bean
        SysLogAdminService sysLogAdminService() {
            return mock(SysLogAdminService.class);
        }

        @Bean
        SysMenuAdminService sysMenuAdminService() {
            return mock(SysMenuAdminService.class);
        }

        @Bean
        SysRoleAdminController sysRoleAdminController(SysRoleAdminService sysRoleAdminService) {
            return new SysRoleAdminController(sysRoleAdminService);
        }

        @Bean
        SysUserAdminController sysUserAdminController(SysUserAdminService sysUserAdminService) {
            return new SysUserAdminController(sysUserAdminService);
        }

        @Bean
        SysNoticeAdminController sysNoticeAdminController(SysNoticeAdminService sysNoticeAdminService) {
            return new SysNoticeAdminController(sysNoticeAdminService);
        }

        @Bean
        SysConfigAdminController sysConfigAdminController(SysConfigAdminService sysConfigAdminService) {
            return new SysConfigAdminController(sysConfigAdminService);
        }

        @Bean
        SysLogAdminController sysLogAdminController(SysLogAdminService sysLogAdminService) {
            return new SysLogAdminController(sysLogAdminService);
        }

        @Bean
        SysMenuAdminController sysMenuAdminController(SysMenuAdminService sysMenuAdminService) {
            return new SysMenuAdminController(sysMenuAdminService);
        }

        @Bean("permission")
        SecurityPermissionChecker securityPermissionChecker() {
            return new SecurityPermissionChecker();
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                    .exceptionHandling(exceptionHandling -> exceptionHandling
                            .authenticationEntryPoint((request, response, exception) ->
                                    HttpServletResponseUtils.writeJson(response,
                                            org.springframework.http.HttpStatus.UNAUTHORIZED.value(),
                                            ResultErrorCode.LOGIN_REQUIRED))
                            .accessDeniedHandler((request, response, exception) ->
                                    HttpServletResponseUtils.writeJson(response,
                                            org.springframework.http.HttpStatus.FORBIDDEN.value(),
                                            ResultErrorCode.FORBIDDEN)));
            return http.build();
        }
    }
}
