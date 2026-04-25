package com.cybzacg.blogbackend.module.follow;

import com.cybzacg.blogbackend.core.security.SecurityPermissionChecker;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.follow.controller.FollowAdminController;
import com.cybzacg.blogbackend.module.follow.controller.PublicFollowController;
import com.cybzacg.blogbackend.module.follow.controller.UserFollowController;
import com.cybzacg.blogbackend.module.follow.model.publics.PublicFollowUserVO;
import com.cybzacg.blogbackend.module.follow.service.FollowAdminService;
import com.cybzacg.blogbackend.module.follow.service.PublicFollowService;
import com.cybzacg.blogbackend.module.follow.service.UserFollowService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = FollowControllerSecurityTest.TestConfig.class)
class FollowControllerSecurityTest {
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private UserFollowService userFollowService;
    @Autowired
    private PublicFollowService publicFollowService;
    @Autowired
    private FollowAdminService followAdminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Mockito.reset(userFollowService, publicFollowService, followAdminService);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void pagePublicFollowsShouldAllowAnonymous() throws Exception {
        when(publicFollowService.pageUserFollows(any(), any()))
                .thenReturn(PageResult.<PublicFollowUserVO>builder()
                        .total(0L)
                        .current(1L)
                        .size(10L)
                        .records(List.of())
                        .build());

        mockMvc.perform(get("/api/users/12/follows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(0));

        verify(publicFollowService).pageUserFollows(org.mockito.ArgumentMatchers.eq(12L), any());
    }

    @Test
    void followUserShouldRequireLogin() throws Exception {
        mockMvc.perform(post("/api/user/follows/12"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.LOGIN_REQUIRED.getCode()));

        verify(userFollowService, never()).followUser(any());
    }

    @Test
    @WithMockUser
    void followUserShouldAllowAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/api/user/follows/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(userFollowService).followUser(12L);
    }

    @Test
    @WithMockUser(authorities = "content:follow:query")
    void adminPageRelationsShouldAllowAuthorizedUser() throws Exception {
        when(followAdminService.pageRelations(any()))
                .thenReturn(PageResult.<com.cybzacg.blogbackend.module.follow.model.admin.FollowAdminRelationVO>builder()
                        .total(0L)
                        .current(1L)
                        .size(10L)
                        .records(List.of())
                        .build());

        mockMvc.perform(get("/api/sys/follows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(followAdminService).pageRelations(any());
    }

    @Test
    @WithMockUser(authorities = "content:follow:query")
    void adminCleanRelationsShouldRejectUserWithoutCleanPermission() throws Exception {
        mockMvc.perform(delete("/api/sys/follows/clean")
                        .contentType(APPLICATION_JSON)
                        .content("{\"cleanInactive\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(followAdminService, never()).cleanRelations(any());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        UserFollowService userFollowService() {
            return mock(UserFollowService.class);
        }

        @Bean
        PublicFollowService publicFollowService() {
            return mock(PublicFollowService.class);
        }

        @Bean
        FollowAdminService followAdminService() {
            return mock(FollowAdminService.class);
        }

        @Bean
        UserFollowController userFollowController(UserFollowService userFollowService) {
            return new UserFollowController(userFollowService);
        }

        @Bean
        PublicFollowController publicFollowController(PublicFollowService publicFollowService) {
            return new PublicFollowController(publicFollowService);
        }

        @Bean
        FollowAdminController followAdminController(FollowAdminService followAdminService) {
            return new FollowAdminController(followAdminService);
        }

        @Bean("permission")
        SecurityPermissionChecker securityPermissionChecker() {
            return new SecurityPermissionChecker();
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers("/api/users/*/follows", "/api/users/*/fans").permitAll()
                            .anyRequest().authenticated())
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
