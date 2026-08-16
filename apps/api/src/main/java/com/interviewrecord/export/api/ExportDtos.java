package com.interviewrecord.export.api;

import com.interviewrecord.preference.domain.Theme;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Explicit, non-sensitive shapes used by the authenticated JSON backup. */
public final class ExportDtos {
    private ExportDtos() {}

    public record UserExport(Long id, String email, String displayName, boolean emailVerified) {}

    public record CompanyExport(Long id, String name, String website, String notes,
            Instant createdAt, Instant updatedAt) {}

    public record JobTypeExport(Long id, String name, boolean active, Instant createdAt, Instant updatedAt) {}

    public record PositionStatusExport(Long id, String name, int sortOrder, String color,
            String statisticsCategory, boolean active, Instant createdAt, Instant updatedAt) {}

    public record PositionExport(Long id, Long companyId, Long jobTypeId, Long statusId, String title,
            String applyUrl, LocalDate appliedAt, Instant deadlineAt, String workLocation,
            String description, boolean archived, Instant createdAt, Instant updatedAt) {}

    public record InterviewRoundExport(Long id, Long positionId, String roundName, int roundNumber,
            String interviewType, Instant startsAt, Instant endsAt, String location, String result,
            String processNotes, String reviewSummary, Instant createdAt, Instant updatedAt) {}

    public record InterviewQuestionExport(Long id, Long roundId, int sortOrder, String question,
            String answer, String category, Instant createdAt, Instant updatedAt) {}

    public record ScheduleExport(Long id, String title, String eventType, Instant startsAt, Instant endsAt,
            Long positionId, Long interviewRoundId, String location, String notes, String status,
            String manualUrgency, String reminderOffsets, Instant createdAt, Instant updatedAt) {}

    public record PreferenceExport(String timeZone, Theme theme, List<Integer> interviewReminderOffsets,
            List<Integer> deadlineReminderOffsets) {}

    public record ExportData(Instant generatedAt, UserExport user, List<CompanyExport> companies,
            List<JobTypeExport> jobTypes, List<PositionStatusExport> statuses, List<PositionExport> positions,
            List<InterviewRoundExport> interviewRounds, List<InterviewQuestionExport> interviewQuestions,
            List<ScheduleExport> schedules, PreferenceExport preferences) {}

    /** 生成导出后的响应：一次性下载令牌（30 分钟有效）与文件名。 */
    public record ExportCreatedResponse(String token, String fileName, Instant expiresAt) {}
}
