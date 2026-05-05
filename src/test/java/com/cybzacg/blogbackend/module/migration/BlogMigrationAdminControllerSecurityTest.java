package com.cybzacg.blogbackend.module.migration;

import com.cybzacg.blogbackend.core.security.SecurityPermissionChecker;
import com.cybzacg.blogbackend.core.util.HttpServletResponseUtils;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.migration.controller.BlogMigrationAdminController;
import com.cybzacg.blogbackend.module.migration.model.admin.BlogMigrationPrecheckResultVO;
import com.cybzacg.blogbackend.module.migration.model.admin.BlogMigrationTaskVO;
import com.cybzacg.blogbackend.module.migration.service.BlogMigrationAdminService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = BlogMigrationAdminControllerSecurityTest.TestConfig.class)
class BlogMigrationAdminControllerSecurityTest {
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private BlogMigrationAdminService blogMigrationAdminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Mockito.reset(blogMigrationAdminService);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(authorities = "content:migration:create")
    void createTaskShouldAllowAuthorizedUser() throws Exception {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            securityUtils.when(SecurityUtils::requireUserId).thenReturn(99L);
            mockMvc.perform(multipart("/api/sys/migrations/blog/tasks")
                            .file("file", "{\"sourcePlatform\":\"wordpress\",\"posts\":[]}".getBytes())
                            .param("authorId", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));
        }
        verify(blogMigrationAdminService).createTask(any(), any(), eq(99L));
    }

    @Test
    @WithMockUser(authorities = "content:migration:query")
    void createTaskShouldRejectUserWithoutCreatePermission() throws Exception {
        mockMvc.perform(multipart("/api/sys/migrations/blog/tasks")
                        .file("file", "{}".getBytes())
                        .param("authorId", "10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(blogMigrationAdminService, never()).createTask(any(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "content:migration:execute")
    void precheckShouldAllowExecutePermission() throws Exception {
        when(blogMigrationAdminService.precheck(eq(1L), any())).thenReturn(new BlogMigrationPrecheckResultVO());
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            securityUtils.when(SecurityUtils::requireUserId).thenReturn(99L);
            mockMvc.perform(post("/api/sys/migrations/blog/tasks/1/precheck"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));
        }
        verify(blogMigrationAdminService).precheck(1L, 99L);
    }

    @Test
    @WithMockUser(authorities = "content:migration:query")
    void executeShouldRejectUserWithoutExecutePermission() throws Exception {
        mockMvc.perform(post("/api/sys/migrations/blog/tasks/1/execute"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(blogMigrationAdminService, never()).execute(any(), any());
    }

    @Test
    @WithMockUser(authorities = "content:migration:query")
    void pageTasksShouldAllowQueryPermission() throws Exception {
        when(blogMigrationAdminService.pageTasks(any())).thenReturn(PageResult.empty());

        mockMvc.perform(get("/api/sys/migrations/blog/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(blogMigrationAdminService).pageTasks(any());
    }

    @Test
    @WithMockUser(authorities = "content:migration:query")
    void getTaskShouldAllowQueryPermission() throws Exception {
        when(blogMigrationAdminService.getTask(1L)).thenReturn(new BlogMigrationTaskVO());

        mockMvc.perform(get("/api/sys/migrations/blog/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(blogMigrationAdminService).getTask(1L);
    }

    @Test
    @WithMockUser(authorities = "content:migration:query")
    void pageRecordsShouldAllowQueryPermission() throws Exception {
        when(blogMigrationAdminService.pageRecords(any())).thenReturn(PageResult.empty());

        mockMvc.perform(get("/api/sys/migrations/blog/tasks/1/records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(blogMigrationAdminService).pageRecords(any());
    }

    @Test
    @WithMockUser(authorities = "content:migration:export")
    void exportFailuresShouldAllowExportPermission() throws Exception {
        when(blogMigrationAdminService.exportFailures(1L)).thenReturn(new byte[]{'P', 'K'});

        mockMvc.perform(get("/api/sys/migrations/blog/tasks/1/failures/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().exists("Content-Disposition"));

        verify(blogMigrationAdminService).exportFailures(1L);
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        BlogMigrationAdminService blogMigrationAdminService() {
            return mock(BlogMigrationAdminService.class);
        }

        @Bean
        BlogMigrationAdminController blogMigrationAdminController(BlogMigrationAdminService blogMigrationAdminService) {
            return new BlogMigrationAdminController(blogMigrationAdminService);
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
