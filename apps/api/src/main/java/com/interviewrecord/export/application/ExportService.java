package com.interviewrecord.export.application;

import com.interviewrecord.auth.domain.User;
import com.interviewrecord.auth.infrastructure.JpaUserRepository;
import com.interviewrecord.export.api.ExportDtos;
import com.interviewrecord.export.api.ExportDtos.CompanyExport;
import com.interviewrecord.export.api.ExportDtos.InterviewQuestionExport;
import com.interviewrecord.export.api.ExportDtos.InterviewRoundExport;
import com.interviewrecord.export.api.ExportDtos.JobTypeExport;
import com.interviewrecord.export.api.ExportDtos.PositionExport;
import com.interviewrecord.export.api.ExportDtos.PositionStatusExport;
import com.interviewrecord.export.api.ExportDtos.PreferenceExport;
import com.interviewrecord.export.api.ExportDtos.ScheduleExport;
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
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

/** Builds a complete user-scoped backup from explicit DTOs, never serializing JPA entities. */
@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class ExportService {
    private final JpaUserRepository users;
    private final JpaCompanyRepository companies;
    private final JpaManagedJobTypeRepository jobTypes;
    private final JpaManagedPositionStatusRepository statuses;
    private final JpaPositionRepository positions;
    private final JpaInterviewRoundRepository rounds;
    private final JpaInterviewQuestionRepository questions;
    private final JpaScheduleEventRepository schedules;
    private final JpaUserPreferenceRepository preferences;
    private final Clock clock;

    public ExportService(JpaUserRepository users, JpaCompanyRepository companies,
            JpaManagedJobTypeRepository jobTypes, JpaManagedPositionStatusRepository statuses,
            JpaPositionRepository positions, JpaInterviewRoundRepository rounds,
            JpaInterviewQuestionRepository questions, JpaScheduleEventRepository schedules,
            JpaUserPreferenceRepository preferences, Clock clock) {
        this.users = users;
        this.companies = companies;
        this.jobTypes = jobTypes;
        this.statuses = statuses;
        this.positions = positions;
        this.rounds = rounds;
        this.questions = questions;
        this.schedules = schedules;
        this.preferences = preferences;
        this.clock = clock;
    }

    public ExportDtos.ExportData export(long userId) {
        User user = users.requireById(userId);
        return new ExportDtos.ExportData(
                Instant.now(clock),
                new ExportDtos.UserExport(user.id(), user.email(), user.displayName(), user.isVerified()),
                companies.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
                        .map(c -> new CompanyExport(c.id(), c.name(), c.website(), c.notes(), c.createdAt(), c.updatedAt()))
                        .toList(),
                jobTypes.findAllByUserIdOrderByIdAsc(userId).stream()
                        .map(t -> new JobTypeExport(t.id(), t.name(), t.active(), t.createdAt(), t.updatedAt()))
                        .toList(),
                statuses.findAllByUserIdOrderBySortOrderAsc(userId).stream()
                        .map(s -> new PositionStatusExport(s.id(), s.name(), s.sortOrder(), s.color(),
                                s.statisticsCategory(), s.active(), s.createdAt(), s.updatedAt()))
                        .toList(),
                positions.findAllByUserId(userId).stream()
                        .map(p -> new PositionExport(p.id(), p.companyId(), p.jobTypeId(), p.statusId(), p.title(),
                                p.applyUrl(), p.appliedAt(), p.deadlineAt(), p.workLocation(), p.description(),
                                p.archived(), p.createdAt(), p.updatedAt()))
                        .toList(),
                rounds.findAllByUserId(userId).stream()
                        .map(r -> new InterviewRoundExport(r.id(), r.positionId(), r.roundName(), r.roundNumber(),
                                r.interviewType(), r.startsAt(), r.endsAt(), r.location(), r.result(), r.processNotes(),
                                r.reviewSummary(), r.createdAt(), r.updatedAt()))
                        .toList(),
                questions.findAllByUserId(userId).stream()
                        .map(q -> new InterviewQuestionExport(q.id(), q.roundId(), q.sortOrder(), q.question(), q.answer(),
                                q.category(), q.createdAt(), q.updatedAt()))
                        .toList(),
                schedules.findAllByUserId(userId).stream()
                        .map(s -> new ScheduleExport(s.id(), s.title(), s.eventType(), s.startsAt(), s.endsAt(),
                                s.positionId(), s.interviewRoundId(), s.location(), s.notes(), s.status(), s.manualUrgency(),
                                s.createdAt(), s.updatedAt()))
                        .toList(),
                preferences.findById(userId)
                        .map(p -> new PreferenceExport(p.timeZone(), p.theme(), p.interviewReminderOffsets(),
                                p.deadlineReminderOffsets()))
                        .orElse(null));
    }
}
