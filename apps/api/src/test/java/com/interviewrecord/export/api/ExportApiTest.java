package com.interviewrecord.export.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewrecord.common.config.SecurityConfig;
import com.interviewrecord.common.error.ExportLinkExpiredException;
import com.interviewrecord.common.error.GlobalExceptionHandler;
import com.interviewrecord.common.security.AuthenticatedUser;
import com.interviewrecord.common.security.CurrentUser;
import com.interviewrecord.common.security.JsonAccessDeniedHandler;
import com.interviewrecord.common.security.JsonAuthenticationEntryPoint;
import com.interviewrecord.export.application.ExportService;
import com.interviewrecord.preference.domain.Theme;
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

@WebMvcTest(value = ExportController.class, properties = "spring.datasource.url=jdbc:test")
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JsonAccessDeniedHandler.class,
        JsonAuthenticationEntryPoint.class, CurrentUser.class})
class ExportApiTest {
    @MockitoBean com.interviewrecord.auth.infrastructure.JpaUserRepository jpaUserRepository;
    @MockitoBean org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private static final AuthenticatedUser ALICE = new AuthenticatedUser(
            42L, "alice@example.com", "Alice", true, "Asia/Shanghai", Theme.GRAPHITE_CORAL);

    @Autowired MockMvc mvc;
    @MockitoBean ExportService exportService;

    @Test
    void exportCreationRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/v1/export/csv").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void csvExportReturnsOneTimeDownloadToken() throws Exception {
        given(exportService.createCsvExport(eq(42L))).willReturn(new ExportDtos.ExportCreatedResponse(
                "token-abc", "interview-record-export-2026-08-13.zip",
                Instant.parse("2026-08-13T00:30:00Z")));

        mvc.perform(post("/api/v1/export/csv").with(authentication(authenticationFor(ALICE))).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token-abc"))
                .andExpect(jsonPath("$.fileName").value("interview-record-export-2026-08-13.zip"))
                .andExpect(jsonPath("$.expiresAt").value("2026-08-13T00:30:00Z"));
    }

    @Test
    void downloadStreamsFileForTheAuthenticatedOwner() throws Exception {
        given(exportService.download(eq(42L), eq("token-abc")))
                .willReturn(new ExportService.ExportDownload("backup.zip", "application/zip", new byte[] {1, 2}));

        mvc.perform(get("/api/v1/export/download/token-abc").with(authentication(authenticationFor(ALICE))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/zip"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment; filename=\"backup.zip\"")));
    }

    @Test
    void expiredOrUsedDownloadLinkReturnsGone() throws Exception {
        given(exportService.download(eq(42L), any())).willThrow(new ExportLinkExpiredException());

        mvc.perform(get("/api/v1/export/download/used-token").with(authentication(authenticationFor(ALICE))))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("EXPORT_LINK_EXPIRED"));
    }

    @Test
    void downloadRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/export/download/token-abc"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jsonExportReusesTheSameOneTimeDownloadFlow() throws Exception {
        given(exportService.createJsonExport(eq(42L))).willReturn(new ExportDtos.ExportCreatedResponse(
                "json-token", "interview-record-export-2026-08-13.json",
                Instant.parse("2026-08-13T00:30:00Z")));

        mvc.perform(post("/api/v1/export/json").with(authentication(authenticationFor(ALICE))).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("json-token"));
    }

    private UsernamePasswordAuthenticationToken authenticationFor(AuthenticatedUser user) {
        return UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities());
    }
}
