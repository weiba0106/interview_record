package com.interviewrecord.tracking.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewrecord.common.config.SecurityConfig;
import com.interviewrecord.common.error.ConflictException;
import com.interviewrecord.common.error.GlobalExceptionHandler;
import com.interviewrecord.common.security.AuthenticatedUser;
import com.interviewrecord.common.security.CurrentUser;
import com.interviewrecord.common.security.JsonAccessDeniedHandler;
import com.interviewrecord.common.security.JsonAuthenticationEntryPoint;
import com.interviewrecord.preference.domain.Theme;
import com.interviewrecord.tracking.application.CompanyService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = CompanyController.class, properties = "spring.datasource.url=jdbc:test")
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JsonAccessDeniedHandler.class,
        JsonAuthenticationEntryPoint.class, CurrentUser.class})
class CompanyApiTest {
    @MockitoBean com.interviewrecord.auth.infrastructure.JpaUserRepository jpaUserRepository;
    @MockitoBean org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private static final AuthenticatedUser ALICE = new AuthenticatedUser(
            42L, "alice@example.com", "Alice", true, "Asia/Shanghai", Theme.GRAPHITE_CORAL);

    @Autowired MockMvc mvc;
    @MockitoBean CompanyService companyService;

    @Test
    void unauthenticatedClientGets401() throws Exception {
        mvc.perform(get("/api/v1/companies"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void createUsesTheAuthenticatedUserNotAnyRequestUserId() throws Exception {
        Instant now = Instant.parse("2026-08-12T00:00:00Z");
        given(companyService.create(eq(42L), any())).willReturn(
                new TrackingDtos.CompanyResponse("1", "字节跳动", null, null, 0, now, now));

        mvc.perform(post("/api/v1/companies").with(authentication(authenticationFor(ALICE))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"字节跳动\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("字节跳动"));
        verify(companyService).create(eq(42L), any());
    }

    @Test
    void duplicateNameReturnsConflictUntilConfirmed() throws Exception {
        given(companyService.create(eq(42L), any()))
                .willThrow(new ConflictException("COMPANY_DUPLICATE", "已存在同名公司，确认后可继续创建"));

        mvc.perform(post("/api/v1/companies").with(authentication(authenticationFor(ALICE))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"字节跳动\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMPANY_DUPLICATE"));
    }

    @Test
    void blankNameIsRejectedWithValidationError() throws Exception {
        mvc.perform(post("/api/v1/companies").with(authentication(authenticationFor(ALICE))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void deleteCompanyWithPositionsRequiresConfirmation() throws Exception {
        org.mockito.Mockito.doThrow(new ConflictException("COMPANY_HAS_POSITIONS", "公司下仍有岗位"))
                .when(companyService).delete(42L, 7L, false);

        mvc.perform(delete("/api/v1/companies/7").with(authentication(authenticationFor(ALICE))).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMPANY_HAS_POSITIONS"));
    }

    @Test
    void unknownOrForeignCompanyIdReturnsStableNotFound() throws Exception {
        given(companyService.get(42L, 99L)).willThrow(new com.interviewrecord.common.error.NotFoundException());

        mvc.perform(get("/api/v1/companies/99").with(authentication(authenticationFor(ALICE))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private UsernamePasswordAuthenticationToken authenticationFor(AuthenticatedUser user) {
        return UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities());
    }
}
