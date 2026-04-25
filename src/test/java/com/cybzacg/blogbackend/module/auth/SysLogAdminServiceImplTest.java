package com.cybzacg.blogbackend.module.auth;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysLog;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.convert.SysLogModelMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysLogAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysLogCleanRequest;
import com.cybzacg.blogbackend.module.auth.model.admin.SysLogPageQuery;
import com.cybzacg.blogbackend.module.auth.repository.SysLogRepository;
import com.cybzacg.blogbackend.module.auth.service.impl.SysLogAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysLogAdminServiceImplTest {
    @Mock
    private SysLogRepository sysLogRepository;
    @Mock
    private SysLogModelMapper sysLogModelMapper;

    private SysLogAdminServiceImpl sysLogAdminService;

    @BeforeEach
    void setUp() {
        sysLogAdminService = new SysLogAdminServiceImpl(sysLogRepository, sysLogModelMapper);
    }

    @Test
    void pageLogsShouldReturnMappedPageResult() {
        SysLogPageQuery query = new SysLogPageQuery();
        query.setCurrent(1L);
        query.setSize(10L);
        query.setModule("auth");

        SysLog log = log(1L, "auth", "/auth/login");
        Page<SysLog> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(log));

        when(sysLogRepository.pageByAdminConditions(query)).thenReturn(page);

        SysLogAdminVO expected = logVO(1L, "auth", "/auth/login");
        when(sysLogModelMapper.toLogVO(log)).thenReturn(expected);

        PageResult<SysLogAdminVO> result = sysLogAdminService.pageLogs(query);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("/auth/login", result.getRecords().get(0).getRequestUri());
    }

    @Test
    void getLogShouldReturnMappedVO() {
        SysLog log = log(1L, "auth", "/auth/login");
        when(sysLogRepository.getById(1L)).thenReturn(log);

        SysLogAdminVO expected = logVO(1L, "auth", "/auth/login");
        when(sysLogModelMapper.toLogVO(log)).thenReturn(expected);

        SysLogAdminVO result = sysLogAdminService.getLog(1L);

        assertEquals(1L, result.getId());
        assertEquals("auth", result.getModule());
    }

    @Test
    void getLogShouldThrowWhenNotFound() {
        when(sysLogRepository.getById(99L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> sysLogAdminService.getLog(99L));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("日志不存在", exception.getMessage());
    }

    @Test
    void deleteLogShouldRemoveLogWhenExists() {
        SysLog log = log(1L, "auth", "/auth/login");
        when(sysLogRepository.getById(1L)).thenReturn(log);

        sysLogAdminService.deleteLog(1L);

        verify(sysLogRepository).removeById(1L);
    }

    @Test
    void cleanLogsShouldRejectWhenNoConditionProvided() {
        SysLogCleanRequest request = new SysLogCleanRequest();

        BusinessException exception = assertThrows(BusinessException.class, () -> sysLogAdminService.cleanLogs(request));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("清理日志必须至少指定一个条件", exception.getMessage());
        verify(sysLogRepository, never()).removeByConditions(request);
    }

    @Test
    void cleanLogsShouldDelegateToRepositoryAndReturnRemovedCount() {
        SysLogCleanRequest request = cleanRequest();
        when(sysLogRepository.removeByConditions(request)).thenReturn(3L);

        long result = sysLogAdminService.cleanLogs(request);

        assertEquals(3L, result);
        verify(sysLogRepository).removeByConditions(request);
    }

    private SysLogCleanRequest cleanRequest() {
        SysLogCleanRequest request = new SysLogCleanRequest();
        request.setModule("auth");
        request.setRequestMethod("POST");
        request.setRequestUri("/auth/login");
        request.setIp("127.0.0.1");
        request.setCreateBy(1L);
        request.setCreateTimeStart(LocalDateTime.ofInstant(Instant.ofEpochMilli(1_000L), ZoneOffset.UTC));
        request.setCreateTimeEnd(LocalDateTime.ofInstant(Instant.ofEpochMilli(2_000L), ZoneOffset.UTC));
        return request;
    }

    private SysLog log(Long id, String module, String requestUri) {
        SysLog log = new SysLog();
        log.setId(id);
        log.setModule(module);
        log.setRequestUri(requestUri);
        log.setRequestMethod("POST");
        return log;
    }

    private SysLogAdminVO logVO(Long id, String module, String requestUri) {
        SysLogAdminVO vo = new SysLogAdminVO();
        vo.setId(id);
        vo.setModule(module);
        vo.setRequestUri(requestUri);
        return vo;
    }
}
