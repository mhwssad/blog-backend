package com.cybzacg.blogbackend.module.follow;

import com.cybzacg.blogbackend.module.follow.controller.UserFollowController;
import com.cybzacg.blogbackend.module.follow.model.user.UserFollowRemarkUpdateRequest;
import com.cybzacg.blogbackend.module.follow.model.user.UserFollowSpecialUpdateRequest;
import com.cybzacg.blogbackend.module.follow.service.UserFollowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserFollowControllerTest {
    @Mock
    private UserFollowService userFollowService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        UserFollowController controller = new UserFollowController(userFollowService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void followUserShouldDelegateToService() throws Exception {
        mockMvc.perform(post("/api/user/follows/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userFollowService).followUser(12L);
    }

    @Test
    void unfollowUserShouldDelegateToService() throws Exception {
        mockMvc.perform(delete("/api/user/follows/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userFollowService).unfollowUser(12L);
    }

    @Test
    void getMutualFollowStatusShouldDelegateToService() throws Exception {
        mockMvc.perform(get("/api/user/follows/mutual").param("targetUserId", "13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userFollowService).getMutualFollowStatus(13L);
    }

    @Test
    void updateSpecialFollowShouldDelegateToService() throws Exception {
        UserFollowSpecialUpdateRequest request = new UserFollowSpecialUpdateRequest();
        request.setSpecialFollow(1);

        mockMvc.perform(put("/api/user/follows/15/special")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userFollowService).updateSpecialFollow(eq(15L),
                argThat(argument -> argument != null && Integer.valueOf(1).equals(argument.getSpecialFollow())));
    }

    @Test
    void updateRemarkShouldDelegateToService() throws Exception {
        UserFollowRemarkUpdateRequest request = new UserFollowRemarkUpdateRequest();
        request.setRemark("前端联调");

        mockMvc.perform(put("/api/user/follows/15/remark")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userFollowService).updateRemark(eq(15L),
                argThat(argument -> argument != null && "前端联调".equals(argument.getRemark())));
    }
}
