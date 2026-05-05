package com.cybzacg.blogbackend.module.forum;

import com.cybzacg.blogbackend.core.security.SecurityPermissionChecker;
import com.cybzacg.blogbackend.core.util.HttpServletResponseUtils;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.forum.controller.ForumSectionAdminController;
import com.cybzacg.blogbackend.module.forum.model.admin.ForumSectionAdminVO;
import com.cybzacg.blogbackend.module.forum.service.ForumSectionAdminService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
@ContextConfiguration(classes = ForumAdminControllerSecurityTest.TestConfig.class)
class ForumAdminControllerSecurityTest {
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private ForumSectionAdminService forumSectionAdminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Mockito.reset(forumSectionAdminService);
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
        ForumSectionAdminController forumSectionAdminController(ForumSectionAdminService forumSectionAdminService) {
            return new ForumSectionAdminController(forumSectionAdminService);
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
