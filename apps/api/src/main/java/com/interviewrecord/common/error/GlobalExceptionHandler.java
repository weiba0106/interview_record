package com.interviewrecord.common.error;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.interviewrecord.auth.application.AuthService;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldViolation::from)
                .collect(Collectors.toMap(
                        FieldViolation::field,
                        FieldViolation::message,
                        (first, ignored) -> first,
                        LinkedHashMap::new));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "请求参数有误", fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception) {
        Map<String, String> fieldErrors = exception.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage(),
                        (first, ignored) -> first,
                        LinkedHashMap::new));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "请求参数有误", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        log.warn("Rejected unreadable request body: {}", exception.getMessage());
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "请求参数有误", Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> handleAuthentication(AuthenticationException exception) {
        return response(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "请先登录", Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception) {
        return response(HttpStatus.FORBIDDEN, "FORBIDDEN", "无权访问此资源", Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleConflict(DataIntegrityViolationException exception) {
        return response(HttpStatus.CONFLICT, "CONFLICT", "资源状态冲突", Map.of());
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiError> handleOptimisticLock(
            org.springframework.orm.ObjectOptimisticLockingFailureException exception) {
        return response(HttpStatus.CONFLICT, "CONCURRENT_UPDATE", "数据已被更新，请刷新后重试", Map.of());
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ResponseEntity<ApiError> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException exception) {
        return response(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "邮箱已注册", Map.of());
    }

    @ExceptionHandler(InvalidRegistrationException.class)
    ResponseEntity<ApiError> handleInvalidRegistration(InvalidRegistrationException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage(), "请求参数有误", Map.of());
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(NotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "资源不存在", Map.of());
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiError> handleBusinessConflict(ConflictException exception) {
        return response(HttpStatus.CONFLICT, exception.code(), exception.getMessage(), Map.of());
    }

    @ExceptionHandler(InvalidInputException.class)
    ResponseEntity<ApiError> handleInvalidInput(InvalidInputException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.code(), exception.getMessage(), Map.of());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<ApiError> handleRateLimit(RateLimitExceededException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(Math.max(1, exception.retryAfter().toSeconds())))
                .body(new ApiError("RATE_LIMITED", "请求过于频繁", Map.of(), MDC.get("traceId")));
    }

    @ExceptionHandler(AuthService.InvalidCredentialsException.class)
    ResponseEntity<ApiError> handleInvalidCredentials(AuthService.InvalidCredentialsException exception) {
        return response(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "邮箱或密码错误", Map.of());
    }

    @ExceptionHandler(AuthService.EmailNotVerifiedException.class)
    ResponseEntity<ApiError> handleEmailNotVerified(AuthService.EmailNotVerifiedException exception) {
        return response(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED", "请先验证邮箱", Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        log.error("Unhandled exception while processing request", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "系统暂时不可用", Map.of());
    }

    private ResponseEntity<ApiError> response(
            HttpStatus status, String code, String message, Map<String, String> fieldErrors) {
        return ResponseEntity.status(status)
                .body(new ApiError(code, message, Map.copyOf(fieldErrors), MDC.get("traceId")));
    }
}
