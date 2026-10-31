package com.taiji.fund.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taiji.fund.common.api.Result;
import com.taiji.fund.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String ERROR_CODE_ATTRIBUTE = "auth.errorCode";

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object attribute = request.getAttribute(ERROR_CODE_ATTRIBUTE);
        ErrorCode errorCode = attribute instanceof ErrorCode ? (ErrorCode) attribute : ErrorCode.UNAUTHORIZED;
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), Result.fail(errorCode));
    }
}
