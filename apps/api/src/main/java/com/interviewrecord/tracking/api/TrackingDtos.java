package com.interviewrecord.tracking.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class TrackingDtos {
    private TrackingDtos() {}

    // ---- Companies ----

    public record CompanyRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 2048) @Pattern(regexp = "^(https?://.*)?$", message = "官网必须以 http:// 或 https:// 开头") String website,
            @Size(max = 2000) String notes,
            Boolean confirmDuplicate) {}

    public record CompanyResponse(String id, String name, String website, String notes,
            long positionCount, Instant createdAt, Instant updatedAt) {}

    public record CompanyDetailResponse(String id, String name, String website, String notes,
            long positionCount, long interviewRoundCount, long scheduleCount,
            List<PositionSummary> positions, Instant createdAt, Instant updatedAt) {}

    // ---- Job types ----

    public record JobTypeRequest(@NotBlank @Size(max = 40) String name, Boolean active) {}

    public record JobTypeResponse(String id, String name, boolean active) {}

    // ---- Statuses ----

    public record StatusRequest(
            @NotBlank @Size(max = 40) String name,
            @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "颜色必须是 #RRGGBB 格式") String color,
            @NotBlank String statisticsCategory,
            Boolean active) {}

    public record StatusReorderRequest(@jakarta.validation.constraints.NotEmpty List<@NotBlank String> orderedIds) {}

    public record StatusResponse(String id, String name, int sortOrder, String color,
            String statisticsCategory, boolean active, long positionCount) {}

    // ---- Positions ----

    public record PositionRequest(
            String companyId,
            @Size(max = 120) String newCompanyName,
            @NotBlank String jobTypeId,
            @NotBlank String statusId,
            @NotBlank @Size(max = 100) String title,
            @Size(max = 2048) String applyUrl,
            LocalDate appliedAt,
            Instant deadlineAt,
            @Size(max = 100) String workLocation,
            /** 富文本 HTML，服务端白名单清洗后存储。 */
            @Size(max = 50000) String description,
            Boolean createDeadlineSchedule,
            Long version) {}

    public record StatusChangeRequest(@NotBlank String statusId) {}

    public record PositionSummary(String id, String title, String companyId, String companyName,
            String jobTypeId, String jobTypeName, StatusRef status, LocalDate appliedAt,
            Instant deadlineAt, boolean archived, Instant updatedAt) {}

    public record StatusRef(String id, String name, String color, String statisticsCategory) {}

    public record NextScheduleRef(String id, String title, String eventType, Instant time) {}

    public record PositionResponse(String id, String title, String companyId, String companyName,
            String jobTypeId, String jobTypeName, StatusRef status, String applyUrl, LocalDate appliedAt,
            Instant deadlineAt, String workLocation, String description, boolean archived,
            long interviewRoundCount, long scheduleCount, NextScheduleRef nextSchedule,
            long version, Instant createdAt, Instant updatedAt) {}

    public record PositionListResponse(List<PositionResponse> items, int page, int size,
            long totalItems, int totalPages) {}
}
