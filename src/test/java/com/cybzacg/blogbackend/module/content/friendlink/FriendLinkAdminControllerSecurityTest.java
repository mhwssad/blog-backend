package com.cybzacg.blogbackend.module.content.friendlink;

import com.cybzacg.blogbackend.core.security.SecurityPermissionChecker;
import com.cybzacg.blogbackend.core.util.HttpServletResponseUtils;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.content.friendlink.controller.FriendLinkAdminController;
import com.cybzacg.blogbackend.module.content.friendlink.controller.PublicFriendLinkController;
import com.cybzacg.blogbackend.module.content.friendlink.service.FriendLinkAdminService;
import com.cybzacg.blogbackend.module.content.friendlink.service.PublicFriendLinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = FriendLinkAdminControllerSecurityTest.TestConfig.class)
class FriendLinkAdminControllerSecurityTest {
    @Autowired
    private WebApplicationContext ctx;
    @Autowired
    private FriendLinkAdminService adminService;
    @Autowired
    private PublicFriendLinkService publicService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Mockito.reset(adminService, publicService);
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
    }

    @Test
    void publicListShouldAllowAnonymous() throws Exception {
        when(publicService.listEnabled()).thenReturn(List.of());

        mockMvc.perform(get("/api/public/friend-links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(publicService).listEnabled();
    }

    @Test
    @WithMockUser(authorities = "content:friend-link:query")
    void adminPageShouldAllowQueryPermission() throws Exception {
        when(adminService.page(any())).thenReturn(PageResult.empty());

        mockMvc.perform(get("/api/sys/friend-links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(adminService).page(any());
    }

    @Test
    @WithMockUser(authorities = "content:friend-link:query")
    void adminCreateShouldRejectWithoutCreatePermission() throws Exception {
        mockMvc.perform(post("/api/sys/friend-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"T\",\"url\":\"https://a.com\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));
    }

    @Test
    @WithMockUser(authorities = "content:friend-link:create")
    void adminCreateShouldAllowCreatePermission() throws Exception {
        when(adminService.create(any())).thenReturn(new com.cybzacg.blogbackend.module.content.friendlink.model.admin.FriendLinkVO());

        mockMvc.perform(post("/api/sys/friend-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"T\",\"url\":\"https://a.com\"}"))
                .andExpect(status().isOk());

        verify(adminService).create(any());
    }

    @Test
    @WithMockUser(authorities = "content:friend-link:update")
    void adminUpdateStatusShouldAllowUpdatePermission() throws Exception {
        mockMvc.perform(put("/api/sys/friend-links/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":0}"))
                .andExpect(status().isOk());

        verify(adminService).updateStatus(1L, 0);
    }

    @Test
    @WithMockUser(authorities = "content:friend-link:delete")
    void adminDeleteShouldAllowDeletePermission() throws Exception {
        mockMvc.perform(delete("/api/sys/friend-links/1"))
                .andExpect(status().isOk());

        verify(adminService).delete(1L);
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        FriendLinkAdminService friendLinkAdminService() {
            return mock(FriendLinkAdminService.class);
        }

        @Bean
        PublicFriendLinkService publicFriendLinkService() {
            return mock(PublicFriendLinkService.class);
        }

        @Bean
        FriendLinkAdminController friendLinkAdminController(FriendLinkAdminService s) {
            return new FriendLinkAdminController(s);
        }

        @Bean
        PublicFriendLinkController publicFriendLinkController(PublicFriendLinkService s) {
            return new PublicFriendLinkController(s);
        }

        @Bean("permission")
        SecurityPermissionChecker securityPermissionChecker() {
            return new SecurityPermissionChecker();
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/public/**").permitAll()
                            .anyRequest().authenticated())
                    .exceptionHandling(eh -> eh
                            .authenticationEntryPoint((req, res, ex) ->
                                    HttpServletResponseUtils.writeJson(res, 401, ResultErrorCode.LOGIN_REQUIRED))
                            .accessDeniedHandler((req, res, ex) ->
                                    HttpServletResponseUtils.writeJson(res, 403, ResultErrorCode.FORBIDDEN)));
            return http.build();
        }
    }
}
