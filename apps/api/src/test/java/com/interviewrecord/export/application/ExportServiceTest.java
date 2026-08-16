package com.interviewrecord.export.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.interviewrecord.auth.domain.User;
import com.interviewrecord.auth.infrastructure.JpaUserRepository;
import com.interviewrecord.common.error.ExportLinkExpiredException;
import com.interviewrecord.common.html.RichTextSanitizer;
import com.interviewrecord.common.token.SecureTokenService;
import com.interviewrecord.export.domain.ExportFile;
import com.interviewrecord.export.infrastructure.JpaExportFileRepository;
import com.interviewrecord.interviews.domain.InterviewRound;
import com.interviewrecord.interviews.infrastructure.JpaInterviewQuestionRepository;
import com.interviewrecord.interviews.infrastructure.JpaInterviewRoundRepository;
import com.interviewrecord.preference.infrastructure.JpaUserPreferenceRepository;
import com.interviewrecord.scheduling.domain.ScheduleEvent;
import com.interviewrecord.scheduling.infrastructure.JpaScheduleEventRepository;
import com.interviewrecord.tracking.domain.Company;
import com.interviewrecord.tracking.domain.JobType;
import com.interviewrecord.tracking.domain.Position;
import com.interviewrecord.tracking.domain.PositionStatus;
import com.interviewrecord.tracking.infrastructure.JpaCompanyRepository;
import com.interviewrecord.tracking.infrastructure.JpaManagedJobTypeRepository;
import com.interviewrecord.tracking.infrastructure.JpaManagedPositionStatusRepository;
import com.interviewrecord.tracking.infrastructure.JpaPositionRepository;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

class ExportServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-13T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final JpaUserRepository users = mock(JpaUserRepository.class);
    private final JpaCompanyRepository companies = mock(JpaCompanyRepository.class);
    private final JpaManagedJobTypeRepository jobTypes = mock(JpaManagedJobTypeRepository.class);
    private final JpaManagedPositionStatusRepository statuses = mock(JpaManagedPositionStatusRepository.class);
    private final JpaPositionRepository positions = mock(JpaPositionRepository.class);
    private final JpaInterviewRoundRepository rounds = mock(JpaInterviewRoundRepository.class);
    private final JpaInterviewQuestionRepository questions = mock(JpaInterviewQuestionRepository.class);
    private final JpaScheduleEventRepository schedules = mock(JpaScheduleEventRepository.class);
    private final JpaUserPreferenceRepository preferences = mock(JpaUserPreferenceRepository.class);
    private final JpaExportFileRepository files = mock(JpaExportFileRepository.class);
    private final SecureTokenService tokens = new SecureTokenService(CLOCK);
    private final ExportService service = new ExportService(users, companies, jobTypes, statuses, positions,
            rounds, questions, schedules, preferences, files, tokens, new RichTextSanitizer(),
            new ObjectMapper(), CLOCK);

    private User user() {
        User user = mock(User.class);
        given(user.id()).willReturn(42L);
        given(user.email()).willReturn("candidate@example.com");
        given(user.displayName()).willReturn("Candidate");
        given(user.isVerified()).willReturn(true);
        given(users.requireById(42L)).willReturn(user);
        return user;
    }

    private void stubEmptyLists() {
        given(companies.findAllByUserIdOrderByUpdatedAtDesc(42L)).willReturn(List.of());
        given(jobTypes.findAllByUserIdOrderByIdAsc(42L)).willReturn(List.of());
        given(statuses.findAllByUserIdOrderBySortOrderAsc(42L)).willReturn(List.of());
        given(positions.findAllByUserId(42L)).willReturn(List.of());
        given(rounds.findAllByUserId(42L)).willReturn(List.of());
        given(questions.findAllByUserId(42L)).willReturn(List.of());
        given(schedules.findAllByUserId(42L)).willReturn(List.of());
        given(preferences.findById(42L)).willReturn(Optional.empty());
    }

    @Test
    void jsonBackupContainsBusinessDataButNoPasswordSessionOrTokenFields() throws Exception {
        user();
        stubEmptyLists();

        String json = new ObjectMapper().writeValueAsString(service.export(42L));

        assertThat(json).contains("candidate@example.com", "generatedAt", "emailVerified");
        assertThat(json).doesNotContain("password", "passwordHash", "session", "token",
                "verification", "reset", "mailInternal");
    }

    @Test
    void csvZipContainsSevenBomCsvFilesWithUserDataAndSanitizedRichText() throws Exception {
        user();
        Company company = new Company(42L, "示例科技", "https://example.com", null, NOW);
        ReflectionTestUtils.setField(company, "id", 1L);
        JobType jobType = new JobType(42L, "秋招", NOW);
        ReflectionTestUtils.setField(jobType, "id", 2L);
        PositionStatus status = new PositionStatus(42L, "投递中", 0, "#46a758", "ACTIVE", NOW);
        ReflectionTestUtils.setField(status, "id", 3L);
        Position position = new Position(42L, 1L, 2L, 3L, "后端开发工程师", null,
                LocalDate.parse("2026-08-01"), null, "上海",
                "<p onclick=\"x()\">岗位描述<script>alert(1)</script></p>", NOW);
        ReflectionTestUtils.setField(position, "id", 4L);
        given(companies.findAllByUserIdOrderByUpdatedAtDesc(42L)).willReturn(List.of(company));
        given(jobTypes.findAllByUserIdOrderByIdAsc(42L)).willReturn(List.of(jobType));
        given(statuses.findAllByUserIdOrderBySortOrderAsc(42L)).willReturn(List.of(status));
        given(positions.findAllByUserId(42L)).willReturn(List.of(position));
        given(rounds.findAllByUserId(42L)).willReturn(List.of());
        given(questions.findAllByUserId(42L)).willReturn(List.of());
        given(schedules.findAllByUserId(42L)).willReturn(List.of());
        given(preferences.findById(42L)).willReturn(Optional.empty());

        service.createCsvExport(42L);

        ArgumentCaptor<ExportFile> capture = ArgumentCaptor.forClass(ExportFile.class);
        verify(files).save(capture.capture());
        byte[] zip = capture.getValue().payload();
        assertThat(capture.getValue().fileName()).isEqualTo("interview-record-export-2026-08-13.zip");
        assertThat(capture.getValue().expiresAt()).isEqualTo(NOW.plusSeconds(30 * 60));

        List<String> names = new java.util.ArrayList<>();
        try (ZipInputStream stream = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            var entry = stream.getNextEntry();
            while (entry != null) {
                names.add(entry.getName());
                stream.closeEntry();
                entry = stream.getNextEntry();
            }
        }
        assertThat(names).containsExactlyInAnyOrder("companies.csv", "job_types.csv", "statuses.csv",
                "positions.csv", "interview_rounds.csv", "interview_questions.csv", "schedules.csv");
        byte[] first = readZipEntry(zip, "positions.csv");
        assertThat(first).startsWith(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        String positionsCsv = new String(first, StandardCharsets.UTF_8);
        assertThat(positionsCsv).contains("示例科技", "秋招", "投递中", "后端开发工程师", "上海");
        assertThat(positionsCsv).doesNotContain("<script>", "onclick");
        assertThat(positionsCsv).contains("<p>岗位描述</p>");
    }

    @Test
    void downloadIsUserScopedExpiryCheckedAndOneTimeOnly() {
        byte[] payload = new byte[] {1, 2, 3};
        byte[] tokenHash = tokens.sha256("raw-token");
        ExportFile file = new ExportFile(42L, tokenHash, "backup.zip", "application/zip", payload,
                NOW.plusSeconds(600), NOW);
        ReflectionTestUtils.setField(file, "id", 7L);
        given(files.findByTokenHashAndUserId(tokenHash, 42L)).willReturn(Optional.of(file));
        given(files.markDownloaded(7L, NOW)).willReturn(1, 0);

        assertThat(service.download(42L, "raw-token").bytes()).containsExactly(1, 2, 3);
        // 第二次下载：令牌已使用
        assertThatThrownBy(() -> service.download(42L, "raw-token"))
                .isInstanceOf(ExportLinkExpiredException.class);
    }

    @Test
    void downloadRejectsAnotherUsersTokenWithoutTouchingTheFile() {
        byte[] tokenHash = tokens.sha256("alice-token");
        given(files.findByTokenHashAndUserId(tokenHash, 43L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.download(43L, "alice-token"))
                .isInstanceOf(ExportLinkExpiredException.class);
        verify(files, never()).markDownloaded(anyLong(), any());
    }

    @Test
    void expiredExportIsRejectedWithoutMarkingDownloaded() {
        byte[] tokenHash = tokens.sha256("expired-token");
        ExportFile file = new ExportFile(42L, tokenHash, "old.zip", "application/zip", new byte[] {9},
                NOW.minusSeconds(1), NOW.minusSeconds(600));
        ReflectionTestUtils.setField(file, "id", 8L);
        given(files.findByTokenHashAndUserId(tokenHash, 42L)).willReturn(Optional.of(file));

        assertThatThrownBy(() -> service.download(42L, "expired-token"))
                .isInstanceOf(ExportLinkExpiredException.class);
        verify(files, never()).markDownloaded(anyLong(), any());
    }

    @Test
    void unknownTokenIsRejected() {
        given(files.findByTokenHashAndUserId(any(), eq(42L))).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.download(42L, "missing-token"))
                .isInstanceOf(ExportLinkExpiredException.class);
    }

    private byte[] readZipEntry(byte[] zip, String name) throws Exception {
        try (ZipInputStream stream = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            var entry = stream.getNextEntry();
            while (entry != null) {
                if (name.equals(entry.getName())) {
                    return stream.readAllBytes();
                }
                stream.closeEntry();
                entry = stream.getNextEntry();
            }
        }
        throw new AssertionError("ZIP 中缺少条目: " + name);
    }
}
