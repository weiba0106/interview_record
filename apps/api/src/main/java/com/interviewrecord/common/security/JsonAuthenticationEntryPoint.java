package com.interviewrecord.common.security;

import tools.jackson.databind.ObjectMapper;
import com.interviewrecord.common.error.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;
    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }
    @Override public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, new ApiError("UNAUTHENTICATED", "请先登录", Map.of(), MDC.get("traceId")));
    }
    static void write(HttpServletResponse response, HttpStatus status, ApiError error) throws IOException {
        response.setStatus(status.value()); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        new ObjectMapper().writeValue(response.getOutputStream(), error);
    }
}
