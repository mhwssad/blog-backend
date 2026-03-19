package com.cybzacg.blogbackend.module.content;

import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.content.controller.UserCollectionController;
import com.cybzacg.blogbackend.module.content.model.user.CollectionFolderVO;
import com.cybzacg.blogbackend.module.content.model.user.CollectionSaveRequest;
import com.cybzacg.blogbackend.module.content.model.user.CollectionVO;
import com.cybzacg.blogbackend.module.content.service.UserCollectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserCollectionServiceTest {
    @Mock
    private UserCollectionService userCollectionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserCollectionController controller = new UserCollectionController(userCollectionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void pageFoldersShouldReturnPageResult() throws Exception {
        CollectionFolderVO folder = new CollectionFolderVO();
        folder.setId(1L);
        folder.setFolderName("默认收藏夹");
        when(userCollectionService.pageFolders())
                .thenReturn(PageResult.<CollectionFolderVO>builder()
                        .total(1L)
                        .current(1L)
                        .size(100L)
                        .records(List.of(folder))
                        .build());

        mockMvc.perform(get("/api/user/collection-folders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].folderName").value("默认收藏夹"));
    }

    @Test
    void createCollectionShouldDelegateToService() throws Exception {
        mockMvc.perform(post("/api/user/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"folderId\": 1,
                                  \"targetId\": 1,
                                  \"targetType\": \"article\",
                                  \"remark\": \"待复习\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userCollectionService).createCollection(any(CollectionSaveRequest.class));
    }
}
