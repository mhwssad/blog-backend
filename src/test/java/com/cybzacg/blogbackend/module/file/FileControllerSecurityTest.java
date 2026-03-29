package com.cybzacg.blogbackend.module.file;

import com.cybzacg.blogbackend.core.security.SecurityPermissionChecker;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.file.controller.FileAdminController;
import com.cybzacg.blogbackend.module.file.controller.UserFileController;
import com.cybzacg.blogbackend.module.file.model.admin.FileAdminVO;
import com.cybzacg.blogbackend.module.file.model.admin.FileDetailVO;
import com.cybzacg.blogbackend.module.file.model.admin.FileTaskAdminVO;
import com.cybzacg.blogbackend.module.file.model.user.FileUploadInitVO;
import com.cybzacg.blogbackend.module.file.model.user.UserFileVO;
import com.cybzacg.blogbackend.module.file.service.FileAdminService;
import com.cybzacg.blogbackend.module.file.service.UserFileService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = FileControllerSecurityTest.TestConfig.class)
class FileControllerSecurityTest {
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private UserFileService userFileService;
    @Autowired
    private FileAdminService fileAdminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Mockito.reset(userFileService, fileAdminService);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void initUploadTaskShouldRequireLogin() throws Exception {
        mockMvc.perform(post("/api/user/files/upload-tasks/init")
                        .contentType(APPLICATION_JSON)
                        .content("{\"originalName\":\"avatar.png\",\"fileSize\":1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.LOGIN_REQUIRED.getCode()));

        verify(userFileService, never()).initUploadTask(any(), any());
    }

    @Test
    @WithMockUser
    void initUploadTaskShouldAllowAuthenticatedUser() throws Exception {
        FileUploadInitVO result = new FileUploadInitVO();
        result.setUploadId("upload-1");
        result.setTaskId(1L);
        when(userFileService.initUploadTask(any(), any())).thenReturn(result);

        mockMvc.perform(post("/api/user/files/upload-tasks/init")
                        .contentType(APPLICATION_JSON)
                        .content("{\"originalName\":\"avatar.png\",\"fileSize\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.uploadId").value("upload-1"));

        verify(userFileService).initUploadTask(any(), any());
    }

    @Test
    void pageMyFilesShouldRequireLogin() throws Exception {
        mockMvc.perform(get("/api/user/files"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.LOGIN_REQUIRED.getCode()));

        verify(userFileService, never()).pageMyFiles(any());
    }

    @Test
    @WithMockUser
    void pageMyFilesShouldAllowAuthenticatedUser() throws Exception {
        when(userFileService.pageMyFiles(any()))
                .thenReturn(PageResult.<UserFileVO>builder()
                        .total(0L)
                        .current(1L)
                        .size(10L)
                        .records(java.util.List.of())
                        .build());

        mockMvc.perform(get("/api/user/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(0));

        verify(userFileService).pageMyFiles(any());
    }

    @Test
    @WithMockUser(authorities = "content:file:query")
    void pageFilesShouldAllowAuthorizedUser() throws Exception {
        when(fileAdminService.pageFiles(any()))
                .thenReturn(PageResult.<FileAdminVO>builder()
                        .total(0L)
                        .current(1L)
                        .size(10L)
                        .records(java.util.List.of())
                        .build());

        mockMvc.perform(get("/api/sys/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(0));

        verify(fileAdminService).pageFiles(any());
    }

    @Test
    @WithMockUser(authorities = "content:file:update")
    void pageFilesShouldRejectUserWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/sys/files"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(fileAdminService, never()).pageFiles(any());
    }

    @Test
    @WithMockUser(authorities = "content:file:query")
    void getFileShouldAllowAuthorizedUser() throws Exception {
        when(fileAdminService.getFile(8L)).thenReturn(new FileDetailVO());

        mockMvc.perform(get("/api/sys/files/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(fileAdminService).getFile(8L);
    }

    @Test
    @WithMockUser(authorities = "content:file:update")
    void getFileShouldRejectUserWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/sys/files/8"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(fileAdminService, never()).getFile(8L);
    }

    @Test
    @WithMockUser(authorities = "content:file:query")
    void pageTasksShouldAllowAuthorizedUser() throws Exception {
        when(fileAdminService.pageTasks(any()))
                .thenReturn(PageResult.<FileTaskAdminVO>builder()
                        .total(0L)
                        .current(1L)
                        .size(10L)
                        .records(java.util.List.of())
                        .build());

        mockMvc.perform(get("/api/sys/files/upload-tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(0));

        verify(fileAdminService).pageTasks(any());
    }

    @Test
    @WithMockUser(authorities = "content:file:delete")
    void pageTasksShouldRejectUserWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/sys/files/upload-tasks"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(fileAdminService, never()).pageTasks(any());
    }

    @Test
    @WithMockUser(authorities = "content:file:update")
    void updateStatusShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(put("/api/sys/files/8/status")
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(fileAdminService).updateStatus(8L, 3);
    }

    @Test
    @WithMockUser(authorities = "content:file:query")
    void updateStatusShouldRejectUserWithoutUpdatePermission() throws Exception {
        mockMvc.perform(put("/api/sys/files/8/status")
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":3}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(fileAdminService, never()).updateStatus(8L, 3);
    }

    @Test
    @WithMockUser(authorities = "content:file:delete")
    void deleteFileShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(delete("/api/sys/files/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(fileAdminService).deleteFile(8L);
    }

    @Test
    @WithMockUser(authorities = "content:file:query")
    void deleteFileShouldRejectUserWithoutDeletePermission() throws Exception {
        mockMvc.perform(delete("/api/sys/files/8"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(fileAdminService, never()).deleteFile(8L);
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        UserFileService userFileService() {
            return mock(UserFileService.class);
        }

        @Bean
        FileAdminService fileAdminService() {
            return mock(FileAdminService.class);
        }

        @Bean
        UserFileController userFileController(UserFileService userFileService) {
            return new UserFileController(userFileService);
        }

        @Bean
        FileAdminController fileAdminController(FileAdminService fileAdminService) {
            return new FileAdminController(fileAdminService);
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
