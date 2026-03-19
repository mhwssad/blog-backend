package com.cybzacg.blogbackend.module.article;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.article.controller.ArticleAdminController;
import com.cybzacg.blogbackend.module.article.model.admin.ArticleAdminVO;
import com.cybzacg.blogbackend.module.article.service.ArticleAdminService;
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
class ArticleAdminControllerTest {
    @Mock
    private ArticleAdminService articleAdminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ArticleAdminController controller = new ArticleAdminController(articleAdminService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(articleAdminService.pageArticles(any()))
                .thenReturn(PageResult.<ArticleAdminVO>builder()
                        .total(0L)
                        .current(1L)
                        .size(10L)
                        .records(List.of())
                        .build());
    }

    @Test
    void pageArticlesShouldReturnPageResult() throws Exception {
        mockMvc.perform(get("/api/sys/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.current").value(1));
    }
}
