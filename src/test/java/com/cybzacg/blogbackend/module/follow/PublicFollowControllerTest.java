package com.cybzacg.blogbackend.module.follow;

import com.cybzacg.blogbackend.module.follow.controller.PublicFollowController;
import com.cybzacg.blogbackend.module.follow.service.PublicFollowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PublicFollowControllerTest {
    @Mock
    private PublicFollowService publicFollowService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PublicFollowController(publicFollowService)).build();
    }

    @Test
    void pageUserFollowsShouldDelegateToService() throws Exception {
        mockMvc.perform(get("/api/users/12/follows")
                        .param("current", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(publicFollowService).pageUserFollows(
                org.mockito.ArgumentMatchers.eq(12L),
                argThat(argument -> argument != null
                        && Long.valueOf(2L).equals(argument.getCurrent())
                        && Long.valueOf(5L).equals(argument.getSize()))
        );
    }

    @Test
    void pageUserFansShouldDelegateToService() throws Exception {
        mockMvc.perform(get("/api/users/12/fans")
                        .param("current", "3")
                        .param("size", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(publicFollowService).pageUserFans(
                org.mockito.ArgumentMatchers.eq(12L),
                argThat(argument -> argument != null
                        && Long.valueOf(3L).equals(argument.getCurrent())
                        && Long.valueOf(8L).equals(argument.getSize()))
        );
    }
}
