package com.interviewrecord.export.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.interviewrecord.auth.domain.User;
import com.interviewrecord.auth.infrastructure.JpaUserRepository;
import com.interviewrecord.interviews.infrastructure.JpaInterviewQuestionRepository;
import com.interviewrecord.interviews.infrastructure.JpaInterviewRoundRepository;
import com.interviewrecord.preference.infrastructure.JpaUserPreferenceRepository;
import com.interviewrecord.scheduling.infrastructure.JpaScheduleEventRepository;
import com.interviewrecord.tracking.infrastructure.JpaCompanyRepository;
import com.interviewrecord.tracking.infrastructure.JpaManagedJobTypeRepository;
import com.interviewrecord.tracking.infrastructure.JpaManagedPositionStatusRepository;
import com.interviewrecord.tracking.infrastructure.JpaPositionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ExportServiceTest {
    private final JpaUserRepository users = mock(JpaUserRepository.class);
    private final JpaCompanyRepository companies = mock(JpaCompanyRepository.class);
    private final JpaManagedJobTypeRepository jobTypes = mock(JpaManagedJobTypeRepository.class);
    private final JpaManagedPositionStatusRepository statuses = mock(JpaManagedPositionStatusRepository.class);
    private final JpaPositionRepository positions = mock(JpaPositionRepository.class);
    private final JpaInterviewRoundRepository rounds = mock(JpaInterviewRoundRepository.class);
    private final JpaInterviewQuestionRepository questions = mock(JpaInterviewQuestionRepository.class);
    private final JpaScheduleEventRepository schedules = mock(JpaScheduleEventRepository.class);
    private final JpaUserPreferenceRepository preferences = mock(JpaUserPreferenceRepository.class);
    private final ExportService service = new ExportService(users, companies, jobTypes, statuses, positions,
            rounds, questions, schedules, preferences,
            Clock.fixed(Instant.parse("2026-08-13T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void jsonBackupContainsBusinessDataButNoPasswordSessionOrTokenFields() throws Exception {
        User user = mock(User.class);
        given(user.id()).willReturn(42L);
        given(user.email()).willReturn("candidate@example.com");
        given(user.displayName()).willReturn("Candidate");
        given(user.isVerified()).willReturn(true);
        given(users.requireById(42L)).willReturn(user);
        given(companies.findAllByUserIdOrderByUpdatedAtDesc(42L)).willReturn(List.of());
        given(jobTypes.findAllByUserIdOrderByIdAsc(42L)).willReturn(List.of());
        given(statuses.findAllByUserIdOrderBySortOrderAsc(42L)).willReturn(List.of());
        given(positions.findAllByUserId(42L)).willReturn(List.of());
        given(rounds.findAllByUserId(42L)).willReturn(List.of());
        given(questions.findAllByUserId(42L)).willReturn(List.of());
        given(schedules.findAllByUserId(42L)).willReturn(List.of());
        given(preferences.findById(42L)).willReturn(Optional.empty());

        String json = new ObjectMapper().writeValueAsString(service.export(42L));

        assertThat(json).contains("candidate@example.com", "generatedAt", "emailVerified");
        assertThat(json).doesNotContain("password", "passwordHash", "session", "token",
                "verification", "reset", "mailInternal");
    }
}
