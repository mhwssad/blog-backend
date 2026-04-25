package com.cybzacg.blogbackend.core.filter;

import com.cybzacg.blogbackend.common.constant.HttpHeaderConstants;
import com.cybzacg.blogbackend.utils.RequestContextUtils;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RequestContextFilterTest {

    @AfterEach
    void tearDown() {
        RequestContextUtils.clear();
    }

    @Test
    void shouldCollectRequestContextAndExposeTraceHeader() throws ServletException, IOException {
        RequestContextFilter filter = new RequestContextFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/articles/1");
        request.setQueryString("preview=true");
        request.addHeader(HttpHeaderConstants.X_TRACE_ID, "trace-from-client");
        request.addHeader(HttpHeaderConstants.USER_AGENT, "JUnit");
        request.setRemoteAddr("192.168.1.8");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceId = new AtomicReference<>();
        AtomicReference<String> clientIp = new AtomicReference<>();
        AtomicReference<String> requestUri = new AtomicReference<>();
        AtomicReference<String> queryString = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> {
            traceId.set(RequestContextUtils.getTraceId());
            clientIp.set(RequestContextUtils.getClientIp());
            requestUri.set(RequestContextUtils.getRequestUri());
            queryString.set(RequestContextUtils.getQueryString());
            assertEquals("trace-from-client", req.getAttribute(RequestContextUtils.TRACE_ID_ATTRIBUTE));
        });

        assertEquals("trace-from-client", traceId.get());
        assertEquals("192.168.1.8", clientIp.get());
        assertEquals("/api/articles/1", requestUri.get());
        assertEquals("preview=true", queryString.get());
        assertEquals("trace-from-client", response.getHeader(HttpHeaderConstants.X_TRACE_ID));
        assertNull(RequestContextUtils.getTraceId());
    }

    @Test
    void shouldGenerateTraceIdWhenHeaderMissing() throws ServletException, IOException {
        RequestContextFilter filter = new RequestContextFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceId = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> traceId.set(RequestContextUtils.getTraceId()));

        assertNotNull(traceId.get());
        assertTrue(!traceId.get().isBlank());
        assertEquals(traceId.get(), response.getHeader(HttpHeaderConstants.X_TRACE_ID));
        assertEquals(traceId.get(), request.getAttribute(RequestContextUtils.TRACE_ID_ATTRIBUTE));
    }

    @Test
    void shouldClearContextWhenFilterChainThrows() {
        RequestContextFilter filter = new RequestContextFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/error");
        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            filter.doFilter(request, response, (req, res) -> {
                assertNotNull(RequestContextUtils.getTraceId());
                throw new IllegalStateException("boom");
            });
        } catch (Exception ex) {
            assertEquals("boom", ex.getMessage());
        }

        assertNull(RequestContextUtils.getTraceId());
        assertNull(RequestContextUtils.getClientIp());
    }
}
