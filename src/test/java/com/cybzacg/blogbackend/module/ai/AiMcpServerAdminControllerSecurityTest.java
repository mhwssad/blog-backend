package com.cybzacg.blogbackend.module.ai;

import com.cybzacg.blogbackend.core.security.SecurityPermissionChecker;
import com.cybzacg.blogbackend.core.util.HttpServletResponseUtils;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.ai.controller.AiMcpServerAdminController;
import com.cybzacg.blogbackend.module.ai.model.admin.AiMcpDiscoverResultVO;
import com.cybzacg.blogbackend.module.ai.service.AiMcpServerAdminService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = AiMcpServerAdminControllerSecurityTest.TestConfig.class)
class AiMcpServerAdminControllerSecurityTest {
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private AiMcpServerAdminService aiMcpServerAdminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Mockito.reset(aiMcpServerAdminService);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(authorities = "ai:mcp:query")
    void pageServersShouldAllowQueryPermission() throws Exception {
        when(aiMcpServerAdminService.pageServers(any())).thenReturn(PageResult.empty());

        mockMvc.perform(get("/api/sys/ai/mcp-servers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(aiMcpServerAdminService).pageServers(any());
    }

    @Test
    @WithMockUser(authorities = "ai:mcp:query")
    void discoverShouldRejectWithoutDiscoverPermission() throws Exception {
        mockMvc.perform(post("/api/sys/ai/mcp-servers/1/discover"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(aiMcpServerAdminService, never()).discoverTools(any(), any());
    }

    @Test
    @WithMockUser(authorities = "ai:mcp:discover")
    void discoverShouldAllowDiscoverPermission() throws Exception {
        AiMcpDiscoverResultVO vo = new AiMcpDiscoverResultVO();
        vo.setDiscoveredCount(1);
        when(aiMcpServerAdminService.discoverTools(eq(1L), eq(99L))).thenReturn(vo);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            securityUtils.when(SecurityUtils::requireUserId).thenReturn(99L);
            mockMvc.perform(post("/api/sys/ai/mcp-servers/1/discover"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));
        }

        verify(aiMcpServerAdminService).discoverTools(1L, 99L);
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        AiMcpServerAdminService aiMcpServerAdminService() {
            return mock(AiMcpServerAdminService.class);
        }

        @Bean
        AiMcpServerAdminController aiMcpServerAdminController(AiMcpServerAdminService aiMcpServerAdminService) {
            return new AiMcpServerAdminController(aiMcpServerAdminService);
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
