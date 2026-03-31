package com.cybzacg.blogbackend.module.follow;

import com.cybzacg.blogbackend.module.follow.controller.FollowAdminController;
import com.cybzacg.blogbackend.module.follow.service.FollowAdminService;
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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FollowAdminControllerTest {
    @Mock
    private FollowAdminService followAdminService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FollowAdminController(followAdminService)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void pageRelationsShouldDelegateToService() throws Exception {
        mockMvc.perform(get("/api/sys/follows")
                        .param("current", "2")
                        .param("size", "20")
                        .param("keyword", "demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(followAdminService).pageRelations(argThat(argument -> argument != null
                && Long.valueOf(2L).equals(argument.getCurrent())
                && Long.valueOf(20L).equals(argument.getSize())
                && "demo".equals(argument.getKeyword())));
    }

    @Test
    void cleanRelationsShouldDelegateToService() throws Exception {
        mockMvc.perform(delete("/api/sys/follows/clean")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CleanRequestPayload(true, false, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(followAdminService).cleanRelations(argThat(argument -> argument != null
                && Boolean.TRUE.equals(argument.getCleanInactive())
                && Boolean.FALSE.equals(argument.getCleanDeletedUsers())
                && Boolean.TRUE.equals(argument.getCleanDisabledUsers())));
    }

    private record CleanRequestPayload(Boolean cleanInactive,
                                       Boolean cleanDeletedUsers,
                                       Boolean cleanDisabledUsers) {
    }
}
