package com.cybzacg.blogbackend.module.file;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.file.controller.FileAdminController;
import com.cybzacg.blogbackend.module.file.model.admin.FileAdminVO;
import com.cybzacg.blogbackend.module.file.service.FileAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FileAdminControllerTest {
    @Mock
    private FileAdminService fileAdminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FileAdminController controller = new FileAdminController(fileAdminService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        when(fileAdminService.pageFiles(any())).thenReturn(PageResult.<FileAdminVO>builder()
                .total(0L)
                .current(1L)
                .size(10L)
                .records(List.of())
                .build());
    }

    @Test
    void pageFilesShouldReturnPageResult() throws Exception {
        mockMvc.perform(get("/api/sys/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
    }
}
