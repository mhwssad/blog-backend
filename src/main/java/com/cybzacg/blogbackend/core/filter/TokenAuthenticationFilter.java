package com.cybzacg.blogbackend.core.filter;

import com.cybzacg.blogbackend.common.constant.HttpHeaderConstants;
import com.cybzacg.blogbackend.enums.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import com.cybzacg.blogbackend.module.auth.token.TokenManager;
import com.cybzacg.blogbackend.utils.HttpServletResponseUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private final TokenManager tokenManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader(HttpHeaderConstants.AUTHORIZATION);
        if (!StringUtils.hasText(token) || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (!tokenManager.validateToken(token)) {
                writeUnauthorized(response, ResultErrorCode.INVALID_TOKEN);
                return;
            }

            Authentication authentication = tokenManager.parseToken(token);
            if (authentication instanceof org.springframework.security.authentication.AbstractAuthenticationToken authToken) {
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            }
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (BusinessException ex) {
            writeUnauthorized(response, ex.getCode(), ex.getMessage());
        }
    }

    private void writeUnauthorized(HttpServletResponse response, ResultErrorCode errorCode) throws IOException {
        HttpServletResponseUtils.writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, errorCode);
    }

    private void writeUnauthorized(HttpServletResponse response, Integer code, String message) throws IOException {
        HttpServletResponseUtils.writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, code, message);
    }
}
