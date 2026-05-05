package com.cybzacg.blogbackend.module.ai;

import com.cybzacg.blogbackend.core.security.SecurityPermissionChecker;
import com.cybzacg.blogbackend.core.util.HttpServletResponseUtils;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.module.ai.controller.AiToolAdminController;
import com.cybzacg.blogbackend.module.ai.model.admin.AiToolExecuteVO;
import com.cybzacg.blogbackend.module.ai.service.AiToolAdminService;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
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
@ContextConfiguration(classes = AiToolAdminControllerSecurityTest.TestConfig.class)
class AiToolAdminControllerSecurityTest {
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private AiToolAdminService aiToolAdminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Mockito.reset(aiToolAdminService);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(authorities = "ai:tool:query")
    void pageToolsShouldAllowQueryPermission() throws Exception {
        when(aiToolAdminService.pageTools(any())).thenReturn(PageResult.empty());

        mockMvc.perform(get("/api/sys/ai/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));

        verify(aiToolAdminService).pageTools(any());
    }

    @Test
    @WithMockUser(authorities = "ai:tool:query")
    void createToolShouldRejectWithoutCreatePermission() throws Exception {
        mockMvc.perform(post("/api/sys/ai/tools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toolCode": "tool-1",
                                  "toolName": "工具1",
                                  "sourceType": "builtin",
                                  "riskLevel": "low",
                                  "enabled": 1
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));

        verify(aiToolAdminService, never()).createTool(any(), any());
    }

    @Test
    @WithMockUser(authorities = "ai:tool:execute")
    void executeToolShouldAllowExecutePermission() throws Exception {
        AiToolExecuteVO vo = new AiToolExecuteVO();
        vo.setSuccess(true);
        when(aiToolAdminService.executeTool(eq(1L), any(), eq(99L))).thenReturn(vo);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            securityUtils.when(SecurityUtils::requireUserId).thenReturn(99L);
            mockMvc.perform(post("/api/sys/ai/tools/1/execute")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"arguments\":\"{}\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()));
        }

        verify(aiToolAdminService).executeTool(eq(1L), any(), eq(99L));
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        AiToolAdminService aiToolAdminService() {
            return mock(AiToolAdminService.class);
        }

        @Bean
        AiToolAdminController aiToolAdminController(AiToolAdminService aiToolAdminService) {
            return new AiToolAdminController(aiToolAdminService);
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
