package com.cybzacg.blogbackend.module.article;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.enums.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.exception.handler.BusinessExceptionHandler;
import com.cybzacg.blogbackend.module.article.controller.PublicArticleController;
import com.cybzacg.blogbackend.module.article.model.publics.PublicArticleCardVO;
import com.cybzacg.blogbackend.module.article.service.PublicArticleService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PublicArticleControllerTest {
    @Mock
    private PublicArticleService publicArticleService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PublicArticleController controller = new PublicArticleController(publicArticleService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new BusinessExceptionHandler())
                .build();
    }

    @Test
    void pageArticlesShouldAllowAnonymousAccess() throws Exception {
        when(publicArticleService.pageArticles(any()))
                .thenReturn(PageResult.<PublicArticleCardVO>builder()
                        .total(0L)
                        .current(1L)
                        .size(10L)
                        .records(List.of())
                        .build());

        mockMvc.perform(get("/api/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void getArticleShouldReturnLoginRequiredCodeWhenServiceRejectsAnonymous() throws Exception {
        when(publicArticleService.getArticle(eq(1L), any(HttpServletRequest.class)))
                .thenThrow(new BusinessException(ResultErrorCode.LOGIN_REQUIRED));

        mockMvc.perform(get("/api/articles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.LOGIN_REQUIRED.getCode()));
    }

    @Test
    void getArticleShouldReturnForbiddenCodeForUnauthorizedUser() throws Exception {
        when(publicArticleService.getArticle(eq(1L), any(HttpServletRequest.class)))
                .thenThrow(new BusinessException(ResultErrorCode.FORBIDDEN.getCode(), "当前用户无权访问该文章"));

        mockMvc.perform(get("/api/articles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultErrorCode.FORBIDDEN.getCode()));
    }
}
