package com.cybzacg.blogbackend.module.forum;

import com.cybzacg.blogbackend.core.security.SecurityPermissionChecker;
import com.cybzacg.blogbackend.core.util.HttpServletResponseUtils;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.forum.controller.ForumPostAdminController;
import com.cybzacg.blogbackend.module.forum.controller.ForumReplyAdminController;
import com.cybzacg.blogbackend.module.forum.controller.ForumSectionAdminController;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumSectionAdminVO;
import com.cybzacg.blogbackend.module.forum.service.ForumPostAdminService;
import com.cybzacg.blogbackend.module.forum.service.ForumReplyAdminService;
import com.cybzacg.blogbackend.module.forum.service.ForumSectionAdminService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.MockedStatic;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
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
@ContextConfiguration(classes = ForumAdminControllerSecurityTest.TestConfig.class)
class ForumAdminControllerSecurityTest {
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private ForumSectionAdminService forumSectionAdminService;
    @Autowired
    private ForumPostAdminService forumPostAdminService;
    @Autowired
    private ForumReplyAdminService forumReplyAdminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Mockito.reset(forumSectionAdminService, forumPostAdminService, forumReplyAdminService);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(authorities = "content:forum:query")
    void pageSectionsShouldAllowAuthorizedUser() throws Exception {
        when(forumSectionAdminService.pageSections(any()))
                .thenReturn(PageResult.<ForumSectionAdminVO>builder()
                        .total(0L)
                        .current(1L)
                        .size(10L)
                        .records(List.of())
                        .build());

        mockMvc.perform(get("/api/sys/forum/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(0));

        verify(forumSectionAdminService).pageSections(any());
    }

    @Test
    @WithMockUser(authorities = "content:forum:update")
    void pageSectionsShouldRejectUserWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/sys/forum/sections"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(forumSectionAdminService, never()).pageSections(any());
    }

    @Test
    @WithMockUser(authorities = "content:forum:query")
    void getSectionShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(get("/api/sys/forum/sections/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(forumSectionAdminService).getSection(10L);
    }

    @Test
    @WithMockUser(authorities = "content:forum:update")
    void getSectionShouldRejectUserWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/sys/forum/sections/10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(forumSectionAdminService, never()).getSection(10L);
    }

    @Test
    @WithMockUser(authorities = "content:forum:create")
    void createSectionShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(post("/api/sys/forum/sections")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Java\",\"visibilityScope\":0,\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(forumSectionAdminService).createSection(any());
    }

    @Test
    @WithMockUser(authorities = "content:forum:query")
    void createSectionShouldRejectUserWithoutCreatePermission() throws Exception {
        mockMvc.perform(post("/api/sys/forum/sections")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Java\",\"visibilityScope\":0,\"status\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(forumSectionAdminService, never()).createSection(any());
    }

    @Test
    @WithMockUser(authorities = "content:forum:update")
    void updateSectionShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(put("/api/sys/forum/sections/10")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Java\",\"visibilityScope\":0,\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(forumSectionAdminService).updateSection(eq(10L), any());
    }

    @Test
    @WithMockUser(authorities = "content:forum:query")
    void updateSectionShouldRejectUserWithoutUpdatePermission() throws Exception {
        mockMvc.perform(put("/api/sys/forum/sections/10")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Java\",\"visibilityScope\":0,\"status\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(forumSectionAdminService, never()).updateSection(eq(10L), any());
    }

    @Test
    @WithMockUser(authorities = "content:forum:update")
    void updateStatusShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(put("/api/sys/forum/sections/10/status")
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(forumSectionAdminService).updateStatus(10L, 0);
    }

    @Test
    @WithMockUser(authorities = "content:forum:query")
    void updateStatusShouldRejectUserWithoutUpdatePermission() throws Exception {
        mockMvc.perform(put("/api/sys/forum/sections/10/status")
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":0}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(forumSectionAdminService, never()).updateStatus(10L, 0);
    }

    @Test
    @WithMockUser(authorities = "content:forum:delete")
    void deleteSectionShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(delete("/api/sys/forum/sections/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(forumSectionAdminService).deleteSection(10L);
    }

    @Test
    @WithMockUser(authorities = "content:forum:query")
    void deleteSectionShouldRejectUserWithoutDeletePermission() throws Exception {
        mockMvc.perform(delete("/api/sys/forum/sections/10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(forumSectionAdminService, never()).deleteSection(10L);
    }

    @Test
    @WithMockUser(authorities = "content:forum:query")
    void pagePostsShouldAllowAuthorizedUser() throws Exception {
        when(forumPostAdminService.pagePosts(any())).thenReturn(PageResult.empty());

        mockMvc.perform(get("/api/sys/forum/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(forumPostAdminService).pagePosts(any());
    }

    @Test
    @WithMockUser(authorities = "content:forum:update")
    void pagePostsShouldRejectUserWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/sys/forum/posts"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(forumPostAdminService, never()).pagePosts(any());
    }

    @Test
    @WithMockUser(authorities = "content:forum:query")
    void getPostShouldAllowAuthorizedUser() throws Exception {
        mockMvc.perform(get("/api/sys/forum/posts/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(forumPostAdminService).getPost(20L);
    }

    @Test
    @WithMockUser(authorities = "content:forum:update")
    void hidePostShouldAllowAuthorizedUser() throws Exception {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            securityUtils.when(SecurityUtils::requireUserId).thenReturn(99L);
            mockMvc.perform(put("/api/sys/forum/posts/20/hide"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));
        }

        verify(forumPostAdminService).hidePost(eq(20L), eq(99L), any(), any());
    }

    @Test
    @WithMockUser(authorities = "content:forum:query")
    void hidePostShouldRejectUserWithoutUpdatePermission() throws Exception {
        mockMvc.perform(put("/api/sys/forum/posts/20/hide"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(forumPostAdminService, never()).hidePost(eq(20L), any(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "content:forum:update")
    void restorePostShouldAllowAuthorizedUser() throws Exception {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            securityUtils.when(SecurityUtils::requireUserId).thenReturn(99L);
            mockMvc.perform(put("/api/sys/forum/posts/20/restore"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));
        }

        verify(forumPostAdminService).restorePost(eq(20L), eq(99L), any(), any());
    }

    @Test
    @WithMockUser(authorities = "content:forum:delete")
    void deletePostShouldAllowAuthorizedUser() throws Exception {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            securityUtils.when(SecurityUtils::requireUserId).thenReturn(99L);
            mockMvc.perform(delete("/api/sys/forum/posts/20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));
        }

        verify(forumPostAdminService).deletePost(eq(20L), eq(99L), any(), any());
    }

    @Test
    @WithMockUser(authorities = "content:forum:query")
    void deletePostShouldRejectUserWithoutDeletePermission() throws Exception {
        mockMvc.perform(delete("/api/sys/forum/posts/20"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(forumPostAdminService, never()).deletePost(eq(20L), any(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "content:forum:update")
    void toggleTopShouldAllowAuthorizedUser() throws Exception {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            securityUtils.when(SecurityUtils::requireUserId).thenReturn(99L);
            mockMvc.perform(put("/api/sys/forum/posts/20/top?enabled=true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));
        }

        verify(forumPostAdminService).toggleTop(eq(20L), eq(true), eq(99L), any(), any());
    }

    @Test
    @WithMockUser(authorities = "content:forum:update")
    void toggleEssenceShouldAllowAuthorizedUser() throws Exception {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            securityUtils.when(SecurityUtils::requireUserId).thenReturn(99L);
            mockMvc.perform(put("/api/sys/forum/posts/20/essence?enabled=true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));
        }

        verify(forumPostAdminService).toggleEssence(eq(20L), eq(true), eq(99L), any(), any());
    }

    @Test
    @WithMockUser(authorities = "content:forum:query")
    void pageRepliesShouldAllowAuthorizedUser() throws Exception {
        when(forumReplyAdminService.pageReplies(any())).thenReturn(PageResult.empty());

        mockMvc.perform(get("/api/sys/forum/replies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(forumReplyAdminService).pageReplies(any());
    }

    @Test
    @WithMockUser(authorities = "content:forum:update")
    void pageRepliesShouldRejectUserWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/sys/forum/replies"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(forumReplyAdminService, never()).pageReplies(any());
    }

    @Test
    @WithMockUser(authorities = "content:forum:update")
    void hideReplyShouldAllowAuthorizedUser() throws Exception {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            securityUtils.when(SecurityUtils::requireUserId).thenReturn(99L);
            mockMvc.perform(put("/api/sys/forum/replies/100/hide"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));
        }

        verify(forumReplyAdminService).hideReply(eq(100L), eq(99L), any(), any());
    }

    @Test
    @WithMockUser(authorities = "content:forum:query")
    void hideReplyShouldRejectUserWithoutUpdatePermission() throws Exception {
        mockMvc.perform(put("/api/sys/forum/replies/100/hide"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(forumReplyAdminService, never()).hideReply(eq(100L), any(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "content:forum:update")
    void restoreReplyShouldAllowAuthorizedUser() throws Exception {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            securityUtils.when(SecurityUtils::requireUserId).thenReturn(99L);
            mockMvc.perform(put("/api/sys/forum/replies/100/restore"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));
        }

        verify(forumReplyAdminService).restoreReply(eq(100L), eq(99L), any(), any());
    }

    @Test
    @WithMockUser(authorities = "content:forum:delete")
    void deleteReplyShouldAllowAuthorizedUser() throws Exception {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            securityUtils.when(SecurityUtils::requireUserId).thenReturn(99L);
            mockMvc.perform(delete("/api/sys/forum/replies/100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));
        }

        verify(forumReplyAdminService).deleteReply(eq(100L), eq(99L), any(), any());
    }

    @Test
    @WithMockUser(authorities = "content:forum:query")
    void deleteReplyShouldRejectUserWithoutDeletePermission() throws Exception {
        mockMvc.perform(delete("/api/sys/forum/replies/100"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(forumReplyAdminService, never()).deleteReply(eq(100L), any(), any(), any());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        ForumSectionAdminService forumSectionAdminService() {
            return mock(ForumSectionAdminService.class);
        }

        @Bean
        ForumPostAdminService forumPostAdminService() {
            return mock(ForumPostAdminService.class);
        }

        @Bean
        ForumReplyAdminService forumReplyAdminService() {
            return mock(ForumReplyAdminService.class);
        }

        @Bean
        ForumSectionAdminController forumSectionAdminController(ForumSectionAdminService forumSectionAdminService) {
            return new ForumSectionAdminController(forumSectionAdminService);
        }

        @Bean
        ForumPostAdminController forumPostAdminController(ForumPostAdminService forumPostAdminService) {
            return new ForumPostAdminController(forumPostAdminService);
        }

        @Bean
        ForumReplyAdminController forumReplyAdminController(ForumReplyAdminService forumReplyAdminService) {
            return new ForumReplyAdminController(forumReplyAdminService);
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
