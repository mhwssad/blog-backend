package com.cybzacg.blogbackend.module.auth;

import com.cybzacg.blogbackend.core.filter.TokenAuthenticationFilter;
import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.token.TokenManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenAuthenticationFilterTest {
    @Mock
    private TokenManager tokenManager;
    @Mock
    private HttpServletRequest request;
    @Mock
    private FilterChain filterChain;

    private TestableTokenAuthenticationFilter filter;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new TestableTokenAuthenticationFilter(tokenManager);
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPassThroughWhenNoAuthorizationHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.invokeFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(tokenManager, never()).validateToken(any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldPassThroughWhenBlankAuthorizationHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("   ");

        filter.invokeFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(tokenManager, never()).validateToken(any());
    }

    @Test
    void shouldPassThroughWhenAlreadyAuthenticated() throws ServletException, IOException {
        SecurityContextHolder.getContext().setAuthentication(mock(Authentication.class));
        when(request.getHeader("Authorization")).thenReturn("Bearer some-token");

        filter.invokeFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(tokenManager, never()).validateToken(any());
    }

    @Test
    void shouldReturn401WhenTokenInvalid() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(tokenManager.validateToken("Bearer invalid-token")).thenReturn(false);

        filter.invokeFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        String body = response.getContentAsString();
        assertEquals(true, body.contains(String.valueOf(ResultErrorCode.INVALID_TOKEN.getCode())));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void shouldSetAuthenticationWhenTokenValid() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(tokenManager.validateToken("Bearer valid-token")).thenReturn(true);

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "user", null, java.util.List.of());
        when(tokenManager.parseToken("Bearer valid-token")).thenReturn(authentication);

        filter.invokeFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(authentication, SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldReturn401WhenParseTokenThrowsBusinessException() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer expired-token");
        when(tokenManager.validateToken("Bearer expired-token")).thenReturn(true);
        when(tokenManager.parseToken("Bearer expired-token"))
                .thenThrow(new BusinessException(40108, "Token无效或已过期"));

        filter.invokeFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        String body = response.getContentAsString();
        assertEquals(true, body.contains("40108"));
        verify(filterChain, never()).doFilter(any(), any());
    }

    private static final class TestableTokenAuthenticationFilter extends TokenAuthenticationFilter {
        private TestableTokenAuthenticationFilter(TokenManager tokenManager) {
            super(tokenManager);
        }

        private void invokeFilter(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
            super.doFilterInternal(request, response, filterChain);
        }
    }
}

