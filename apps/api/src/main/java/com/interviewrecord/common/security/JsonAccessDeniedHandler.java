package com.interviewrecord.common.security;

import tools.jackson.databind.ObjectMapper;
import com.interviewrecord.common.error.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;
    public JsonAccessDeniedHandler(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }
    @Override public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) throws IOException {
        String code = exception instanceof CsrfException ? "INVALID_CSRF_TOKEN" : "FORBIDDEN";
        String message = exception instanceof CsrfException ? "CSRF 令牌无效" : "无权访问此资源";
        response.setStatus(HttpStatus.FORBIDDEN.value()); response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), new ApiError(code, message, Map.of(), MDC.get("traceId")));
    }
}
