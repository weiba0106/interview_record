package com.interviewrecord.export.application;

import com.interviewrecord.auth.domain.User;
import com.interviewrecord.auth.infrastructure.JpaUserRepository;
import com.interviewrecord.common.error.ExportLinkExpiredException;
import com.interviewrecord.common.html.RichTextSanitizer;
import com.interviewrecord.common.token.IssuedToken;
import com.interviewrecord.common.token.SecureTokenService;
import com.interviewrecord.export.api.ExportDtos;
import com.interviewrecord.export.api.ExportDtos.CompanyExport;
import com.interviewrecord.export.api.ExportDtos.InterviewQuestionExport;
import com.interviewrecord.export.api.ExportDtos.InterviewRoundExport;
import com.interviewrecord.export.api.ExportDtos.JobTypeExport;
import com.interviewrecord.export.api.ExportDtos.PositionExport;
import com.interviewrecord.export.api.ExportDtos.PositionStatusExport;
import com.interviewrecord.export.api.ExportDtos.PreferenceExport;
import com.interviewrecord.export.api.ExportDtos.ScheduleExport;
import com.interviewrecord.export.domain.ExportFile;
import com.interviewrecord.export.infrastructure.JpaExportFileRepository;
import com.interviewrecord.interviews.infrastructure.JpaInterviewQuestionRepository;
import com.interviewrecord.interviews.infrastructure.JpaInterviewRoundRepository;
import com.interviewrecord.preference.infrastructure.JpaUserPreferenceRepository;
import com.interviewrecord.scheduling.infrastructure.JpaScheduleEventRepository;
import com.interviewrecord.tracking.domain.Company;
import com.interviewrecord.tracking.domain.JobType;
import com.interviewrecord.tracking.domain.PositionStatus;
import com.interviewrecord.tracking.infrastructure.JpaCompanyRepository;
import com.interviewrecord.tracking.infrastructure.JpaManagedJobTypeRepository;
import com.interviewrecord.tracking.infrastructure.JpaManagedPositionStatusRepository;
import com.interviewrecord.tracking.infrastructure.JpaPositionRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 鏋勫缓瀹屾暣鐢ㄦ埛鑼冨洿澶囦唤骞惰惤鐩樹负涓€娆℃€т笅杞芥枃浠讹細
 * - CSV ZIP 涓?JSON 鍧囬€氳繃 POST 鐢熸垚銆丟ET /download/{token} 涓嬭浇锛? * - 涓嬭浇鍦板潃 30 鍒嗛挓杩囨湡锛屼笅杞戒竴娆″悗绔嬪嵆澶辨晥锛? * - 浠ょ墝鍙繚瀛?SHA-256 鎽樿锛屾寜鐢ㄦ埛闅旂銆? */
@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class ExportService {
    private static final Duration DOWNLOAD_LIFETIME = Duration.ofMinutes(30);

    private final JpaUserRepository users;
    private final JpaCompanyRepository companies;
    private final JpaManagedJobTypeRepository jobTypes;
    private final JpaManagedPositionStatusRepository statuses;
    private final JpaPositionRepository positions;
    private final JpaInterviewRoundRepository rounds;
    private final JpaInterviewQuestionRepository questions;
    private final JpaScheduleEventRepository schedules;
    private final JpaUserPreferenceRepository preferences;
    private final JpaExportFileRepository files;
    private final SecureTokenService tokens;
    private final RichTextSanitizer sanitizer;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ExportService(JpaUserRepository users, JpaCompanyRepository companies,
            JpaManagedJobTypeRepository jobTypes, JpaManagedPositionStatusRepository statuses,
            JpaPositionRepository positions, JpaInterviewRoundRepository rounds,
            JpaInterviewQuestionRepository questions, JpaScheduleEventRepository schedules,
            JpaUserPreferenceRepository preferences, JpaExportFileRepository files,
            SecureTokenService tokens, RichTextSanitizer sanitizer, ObjectMapper objectMapper, Clock clock) {
        this.users = users;
        this.companies = companies;
        this.jobTypes = jobTypes;
        this.statuses = statuses;
        this.positions = positions;
        this.rounds = rounds;
        this.questions = questions;
        this.schedules = schedules;
        this.preferences = preferences;
        this.files = files;
        this.tokens = tokens;
        this.sanitizer = sanitizer;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public ExportDtos.ExportData export(long userId) {
        User user = users.requireById(userId);
        List<Company> companyRows = companies.findAllByUserIdOrderByUpdatedAtDesc(userId);
        List<JobType> jobTypeRows = jobTypes.findAllByUserIdOrderByIdAsc(userId);
        List<PositionStatus> statusRows = statuses.findAllByUserIdOrderBySortOrderAsc(userId);
        return new ExportDtos.ExportData(
                Instant.now(clock),
                new ExportDtos.UserExport(user.id(), user.email(), user.displayName(), user.isVerified()),
                companyRows.stream()
                        .map(c -> new CompanyExport(c.id(), c.name(), c.website(), c.notes(), c.createdAt(), c.updatedAt()))
                        .toList(),
                jobTypeRows.stream()
                        .map(t -> new JobTypeExport(t.id(), t.name(), t.active(), t.createdAt(), t.updatedAt()))
                        .toList(),
                statusRows.stream()
                        .map(s -> new PositionStatusExport(s.id(), s.name(), s.sortOrder(), s.color(),
                                s.statisticsCategory(), s.active(), s.createdAt(), s.updatedAt()))
                        .toList(),
                positions.findAllByUserId(userId).stream()
                        .map(p -> new PositionExport(p.id(), p.companyId(), p.jobTypeId(), p.statusId(), p.title(),
                                p.applyUrl(), p.appliedAt(), p.deadlineAt(), p.workLocation(),
                                sanitizer.sanitize(p.description()),
                                p.archived(), p.createdAt(), p.updatedAt()))
                        .toList(),
                rounds.findAllByUserId(userId).stream()
                        .map(r -> new InterviewRoundExport(r.id(), r.positionId(), r.roundName(), r.roundNumber(),
                                r.interviewType(), r.startsAt(), r.endsAt(), r.location(), r.result(),
                                sanitizer.sanitize(r.processNotes()), sanitizer.sanitize(r.reviewSummary()),
                                r.createdAt(), r.updatedAt()))
                        .toList(),
                questions.findAllByUserId(userId).stream()
                        .map(q -> new InterviewQuestionExport(q.id(), q.roundId(), q.sortOrder(), q.question(), q.answer(),
                                q.category(), q.createdAt(), q.updatedAt()))
                        .toList(),
                schedules.findAllByUserId(userId).stream()
                        .map(s -> new ScheduleExport(s.id(), s.title(), s.eventType(), s.startsAt(), s.endsAt(),
                                s.positionId(), s.interviewRoundId(), s.location(), s.notes(), s.status(), s.manualUrgency(),
                                s.reminderOffsets(), s.createdAt(), s.updatedAt()))
                        .toList(),
                preferences.findById(userId)
                        .map(p -> new PreferenceExport(p.timeZone(), p.theme(), p.interviewReminderOffsets(),
                                p.deadlineReminderOffsets()))
                        .orElse(null));
    }

    @Transactional
    public ExportDtos.ExportCreatedResponse createCsvExport(Long userId) {
        ExportDtos.ExportData data = export(userId);
        byte[] zip = CsvExportWriter.zip(csvEntries(data));
        String date = LocalDate.now(clock).toString();
        return store(userId, "interview-record-export-" + date + ".zip", "application/zip", zip);
    }

    @Transactional
    public ExportDtos.ExportCreatedResponse createJsonExport(Long userId) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(export(userId));
            String date = LocalDate.now(clock).toString();
            return store(userId, "interview-record-export-" + date + ".json", "application/json", json);
        } catch (JacksonException exception) {
            throw new UncheckedIOException("Unable to serialize JSON export",
                    new IOException(exception.getMessage(), exception));
        }
    }

    /** 涓€娆℃€т笅杞斤細鏍￠獙鐢ㄦ埛銆佹湁鏁堟湡涓庢湭浣跨敤鐘舵€侊紝鎴愬姛鍗充綔搴熴€?*/
    @Transactional
    public ExportDownload download(Long userId, String rawToken) {
        ExportFile file = files.findByTokenHashAndUserId(tokens.sha256(rawToken), userId)
                .orElseThrow(ExportLinkExpiredException::new);
        Instant now = clock.instant();
        if (file.downloadedAt() != null || file.expiresAt().isBefore(now)) {
            throw new ExportLinkExpiredException();
        }
        if (files.markDownloaded(file.id(), now) == 0) {
            throw new ExportLinkExpiredException();
        }
        return new ExportDownload(file.fileName(), file.contentType(), file.payload());
    }

    @Transactional
    public long removeExpired() {
        return files.deleteAllByExpiresAtBefore(clock.instant());
    }

    private ExportDtos.ExportCreatedResponse store(Long userId, String fileName, String contentType, byte[] payload) {
        IssuedToken token = tokens.issue(DOWNLOAD_LIFETIME);
        files.save(new ExportFile(userId, token.sha256(), fileName, contentType, payload,
                token.expiresAt(), clock.instant()));
        return new ExportDtos.ExportCreatedResponse(token.rawValue(), fileName, token.expiresAt());
    }

    private Map<String, byte[]> csvEntries(ExportDtos.ExportData data) {
        Map<Long, String> companyNames = data.companies().stream()
                .collect(Collectors.toMap(CompanyExport::id, CompanyExport::name));
        Map<Long, String> jobTypeNames = data.jobTypes().stream()
                .collect(Collectors.toMap(JobTypeExport::id, JobTypeExport::name));
        Map<Long, String> statusNames = data.statuses().stream()
                .collect(Collectors.toMap(PositionStatusExport::id, PositionStatusExport::name));

        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("companies.csv", CsvExportWriter.csv(
                List.of("id", "name", "website", "notes", "created_at", "updated_at"),
                data.companies().stream().map(c -> java.util.Arrays.asList(
                        c.id(), c.name(), c.website(), c.notes(), c.createdAt(), c.updatedAt())).toList()));
        entries.put("job_types.csv", CsvExportWriter.csv(
                List.of("id", "name", "active", "created_at", "updated_at"),
                data.jobTypes().stream().map(t -> java.util.Arrays.asList(
                        t.id(), t.name(), t.active(), t.createdAt(), t.updatedAt())).toList()));
        entries.put("statuses.csv", CsvExportWriter.csv(
                List.of("id", "name", "sort_order", "color", "statistics_category", "active", "created_at", "updated_at"),
                data.statuses().stream().map(s -> java.util.Arrays.asList(
                        s.id(), s.name(), s.sortOrder(), s.color(), s.statisticsCategory(), s.active(),
                        s.createdAt(), s.updatedAt())).toList()));
        entries.put("positions.csv", CsvExportWriter.csv(
                List.of("id", "company_id", "company_name", "job_type_id", "job_type_name", "status_id", "status_name",
                        "title", "apply_url", "applied_at", "deadline_at", "work_location", "description",
                        "archived", "created_at", "updated_at"),
                data.positions().stream().map(p -> java.util.Arrays.asList(
                        p.id(), p.companyId(), companyNames.getOrDefault(p.companyId(), ""),
                        p.jobTypeId(), jobTypeNames.getOrDefault(p.jobTypeId(), ""),
                        p.statusId(), statusNames.getOrDefault(p.statusId(), ""),
                        p.title(), p.applyUrl(), p.appliedAt(), p.deadlineAt(), p.workLocation(),
                        p.description(), p.archived(), p.createdAt(), p.updatedAt())).toList()));
        entries.put("interview_rounds.csv", CsvExportWriter.csv(
                List.of("id", "position_id", "round_name", "round_number", "interview_type", "starts_at", "ends_at",
                        "location", "result", "process_notes", "review_summary", "created_at", "updated_at"),
                data.interviewRounds().stream().map(r -> java.util.Arrays.asList(
                        r.id(), r.positionId(), r.roundName(), r.roundNumber(), r.interviewType(), r.startsAt(),
                        r.endsAt(), r.location(), r.result(), r.processNotes(), r.reviewSummary(),
                        r.createdAt(), r.updatedAt())).toList()));
        entries.put("interview_questions.csv", CsvExportWriter.csv(
                List.of("id", "round_id", "sort_order", "question", "answer", "category", "created_at", "updated_at"),
                data.interviewQuestions().stream().map(q -> java.util.Arrays.asList(
                        q.id(), q.roundId(), q.sortOrder(), q.question(), q.answer(), q.category(),
                        q.createdAt(), q.updatedAt())).toList()));
        entries.put("schedules.csv", CsvExportWriter.csv(
                List.of("id", "title", "event_type", "starts_at", "ends_at", "position_id", "interview_round_id",
                        "location", "notes", "status", "manual_urgency", "reminder_offsets", "created_at", "updated_at"),
                data.schedules().stream().map(s -> java.util.Arrays.asList(
                        s.id(), s.title(), s.eventType(), s.startsAt(), s.endsAt(), s.positionId(),
                        s.interviewRoundId(), s.location(), s.notes(), s.status(), s.manualUrgency(),
                        s.reminderOffsets(), s.createdAt(), s.updatedAt())).toList()));
        return entries;
    }

    /** 涓嬭浇鍝嶅簲锛氭枃浠跺悕銆佸唴瀹圭被鍨嬩笌鏂囦欢瀛楄妭銆?*/
    public record ExportDownload(String fileName, String contentType, byte[] bytes) {}
}

