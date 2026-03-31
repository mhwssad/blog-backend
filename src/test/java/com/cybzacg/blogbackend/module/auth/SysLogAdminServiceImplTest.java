package com.cybzacg.blogbackend.module.auth;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cybzacg.blogbackend.core.web.PageResult;
import com.cybzacg.blogbackend.domain.SysLog;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.convert.SysLogModelMapper;
import com.cybzacg.blogbackend.module.auth.model.admin.SysLogAdminVO;
import com.cybzacg.blogbackend.module.auth.model.admin.SysLogCleanRequest;
import com.cybzacg.blogbackend.module.auth.model.admin.SysLogPageQuery;
import com.cybzacg.blogbackend.module.auth.service.SysLogService;
import com.cybzacg.blogbackend.module.auth.service.impl.SysLogAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysLogAdminServiceImplTest {
    @Mock
    private SysLogService sysLogService;
    @Mock
    private SysLogModelMapper sysLogModelMapper;
    @Mock
    private LambdaQueryChainWrapper<SysLog> logQuery;
    @Mock
    private LambdaUpdateChainWrapper<SysLog> logUpdate;

    private SysLogAdminServiceImpl sysLogAdminService;

    @BeforeEach
    void setUp() {
        sysLogAdminService = new SysLogAdminServiceImpl(sysLogService, sysLogModelMapper);
    }

    @Test
    void pageLogsShouldReturnMappedPageResult() {
        SysLogPageQuery query = new SysLogPageQuery();
        query.setCurrent(1L);
        query.setSize(10L);
        query.setModule("auth");
        query.setRequestMethod("POST");
        query.setRequestUri("/auth/login");
        query.setIp("127.0.0.1");
        query.setCreateBy(1L);
        query.setCreateTimeStart(new Date(1_000L));
        query.setCreateTimeEnd(new Date(2_000L));

        SysLog log = log(1L, "auth", "/auth/login");
        Page<SysLog> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(log));

        when(sysLogService.lambdaQuery()).thenReturn(logQuery);
        stubPagedLogQueryChain();
        when(logQuery.page(any())).thenReturn(page);

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
        when(sysLogService.getById(1L)).thenReturn(log);

        SysLogAdminVO expected = logVO(1L, "auth", "/auth/login");
        when(sysLogModelMapper.toLogVO(log)).thenReturn(expected);

        SysLogAdminVO result = sysLogAdminService.getLog(1L);

        assertEquals(1L, result.getId());
        assertEquals("auth", result.getModule());
    }

    @Test
    void getLogShouldThrowWhenNotFound() {
        when(sysLogService.getById(99L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> sysLogAdminService.getLog(99L));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("日志不存在", exception.getMessage());
    }

    @Test
    void deleteLogShouldRemoveLogWhenExists() {
        SysLog log = log(1L, "auth", "/auth/login");
        when(sysLogService.getById(1L)).thenReturn(log);

        sysLogAdminService.deleteLog(1L);

        verify(sysLogService).removeById(1L);
    }

    @Test
    void cleanLogsShouldRejectWhenNoConditionProvided() {
        SysLogCleanRequest request = new SysLogCleanRequest();

        BusinessException exception = assertThrows(BusinessException.class, () -> sysLogAdminService.cleanLogs(request));

        assertEquals(ResultErrorCode.ILLEGAL_ARGUMENT.getCode(), exception.getCode());
        assertEquals("清理日志必须至少指定一个条件", exception.getMessage());
        verify(sysLogService, never()).lambdaQuery();
        verify(sysLogService, never()).lambdaUpdate();
    }

    @Test
    void cleanLogsShouldReturnZeroWhenNothingMatches() {
        SysLogCleanRequest request = cleanRequest();
        when(sysLogService.lambdaQuery()).thenReturn(logQuery);
        stubCleanLogQueryChain();
        when(logQuery.count()).thenReturn(0L);

        long result = sysLogAdminService.cleanLogs(request);

        assertEquals(0L, result);
        verify(sysLogService, never()).lambdaUpdate();
    }

    @Test
    void cleanLogsShouldRemoveMatchedLogsAndReturnCount() {
        SysLogCleanRequest request = cleanRequest();
        when(sysLogService.lambdaQuery()).thenReturn(logQuery);
        stubCleanLogQueryChain();
        when(logQuery.count()).thenReturn(3L);

        when(sysLogService.lambdaUpdate()).thenReturn(logUpdate);
        stubLogUpdateChain();

        long result = sysLogAdminService.cleanLogs(request);

        assertEquals(3L, result);
        verify(logUpdate).remove();
    }

    private void stubPagedLogQueryChain() {
        when(logQuery.like(anyBoolean(), any(SFunction.class), any())).thenReturn(logQuery);
        when(logQuery.eq(anyBoolean(), any(SFunction.class), any())).thenReturn(logQuery);
        when(logQuery.ge(anyBoolean(), any(SFunction.class), any())).thenReturn(logQuery);
        when(logQuery.le(anyBoolean(), any(SFunction.class), any())).thenReturn(logQuery);
        when(logQuery.orderByDesc(any(SFunction.class))).thenReturn(logQuery);
    }

    private void stubCleanLogQueryChain() {
        when(logQuery.like(anyBoolean(), any(SFunction.class), any())).thenReturn(logQuery);
        when(logQuery.eq(anyBoolean(), any(SFunction.class), any())).thenReturn(logQuery);
        when(logQuery.ge(anyBoolean(), any(SFunction.class), any())).thenReturn(logQuery);
        when(logQuery.le(anyBoolean(), any(SFunction.class), any())).thenReturn(logQuery);
    }

    private void stubLogUpdateChain() {
        when(logUpdate.like(anyBoolean(), any(SFunction.class), any())).thenReturn(logUpdate);
        when(logUpdate.eq(anyBoolean(), any(SFunction.class), any())).thenReturn(logUpdate);
        when(logUpdate.ge(anyBoolean(), any(SFunction.class), any())).thenReturn(logUpdate);
        when(logUpdate.le(anyBoolean(), any(SFunction.class), any())).thenReturn(logUpdate);
        when(logUpdate.remove()).thenReturn(true);
    }

    private SysLogCleanRequest cleanRequest() {
        SysLogCleanRequest request = new SysLogCleanRequest();
        request.setModule("auth");
        request.setRequestMethod("POST");
        request.setRequestUri("/auth/login");
        request.setIp("127.0.0.1");
        request.setCreateBy(1L);
        request.setCreateTimeStart(new Date(1_000L));
        request.setCreateTimeEnd(new Date(2_000L));
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
