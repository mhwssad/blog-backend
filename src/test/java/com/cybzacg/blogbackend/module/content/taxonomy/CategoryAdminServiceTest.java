package com.cybzacg.blogbackend.module.content.taxonomy;

import com.cybzacg.blogbackend.module.content.taxonomy.controller.ContentCategoryAdminController;
import com.cybzacg.blogbackend.module.content.taxonomy.model.admin.CategoryTreeVO;
import com.cybzacg.blogbackend.module.content.taxonomy.service.CategoryAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CategoryAdminServiceTest {
    @Mock
    private CategoryAdminService categoryAdminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ContentCategoryAdminController controller = new ContentCategoryAdminController(categoryAdminService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listCategoryTreeShouldReturnStructuredResult() throws Exception {
        CategoryTreeVO root = new CategoryTreeVO();
        root.setId(1L);
        root.setName("技术分享");
        root.setChildren(List.of());
        when(categoryAdminService.listCategoryTree()).thenReturn(List.of(root));

        mockMvc.perform(get("/api/sys/categories/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("技术分享"));
    }
}
