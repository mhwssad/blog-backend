package com.cybzacg.blogbackend.module.file;

import com.cybzacg.blogbackend.config.property.FileUploadProperties;
import com.cybzacg.blogbackend.core.validation.AllowedFileExtensionValidator;
import com.cybzacg.blogbackend.core.validation.MaxUploadFileSizeValidator;
import com.cybzacg.blogbackend.core.validation.Md5RequiredValidator;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.module.file.controller.UserFileController;
import com.cybzacg.blogbackend.module.file.model.user.FileUploadInitRequest;
import com.cybzacg.blogbackend.module.file.model.user.FileUploadInitVO;
import com.cybzacg.blogbackend.module.file.model.user.UserFileVO;
import com.cybzacg.blogbackend.module.file.service.UserFileService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserFileControllerTest {
    @Mock
    private UserFileService userFileService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserFileController controller = new UserFileController(userFileService);
        FileUploadProperties fileUploadProperties = new FileUploadProperties();
        MaxUploadFileSizeValidator maxUploadFileSizeValidator = new MaxUploadFileSizeValidator(fileUploadProperties);
        Md5RequiredValidator md5RequiredValidator = new Md5RequiredValidator(fileUploadProperties);
        AllowedFileExtensionValidator allowedFileExtensionValidator = new AllowedFileExtensionValidator(fileUploadProperties);
        Map<Class<? extends ConstraintValidator<?, ?>>, ConstraintValidator<?, ?>> validatorMap = Map.of(
                MaxUploadFileSizeValidator.class, maxUploadFileSizeValidator,
                Md5RequiredValidator.class, md5RequiredValidator,
                AllowedFileExtensionValidator.class, allowedFileExtensionValidator
        );
        ConstraintValidatorFactory defaultFactory = new ConstraintValidatorFactoryImpl();
        ConstraintValidatorFactory customFactory = new ConstraintValidatorFactory() {
            @Override
            public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
                @SuppressWarnings("unchecked")
                T instance = (T) validatorMap.get(key);
                return instance != null ? instance : defaultFactory.getInstance(key);
            }
            @Override
            public void releaseInstance(ConstraintValidator<?, ?> instance) {
                // no-op
            }
        };
        LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();
        validatorFactoryBean.setConstraintValidatorFactory(customFactory);
        validatorFactoryBean.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validatorFactoryBean)
                .build();
    }

    @Test
    void initUploadTaskShouldReturnResult() throws Exception {
        FileUploadInitVO initVO = new FileUploadInitVO();
        initVO.setUploadId("u1");
        initVO.setTaskId(1L);
        when(userFileService.initUploadTask(any(FileUploadInitRequest.class), nullable(String.class))).thenReturn(initVO);

        mockMvc.perform(post("/api/user/files/upload-tasks/init")
                        .contentType(APPLICATION_JSON)
                        .content("{\"originalName\":\"a.png\",\"fileSize\":1,\"fileMd5\":\"d41d8cd98f00b204e9800998ecf8427e\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.uploadId").value("u1"));
    }

    @Test
    void pageMyFilesShouldReturnPageResult() throws Exception {
        when(userFileService.pageMyFiles(any())).thenReturn(PageResult.<UserFileVO>builder()
                .total(0L)
                .current(1L)
                .size(10L)
                .records(List.of())
                .build());

        mockMvc.perform(get("/api/user/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
    }
}
