package com.interviewrecord.interviews.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class InterviewDtos {
    private InterviewDtos() {}

    public record QuestionItem(
            @NotBlank @Size(max = 2000) String question,
            @Size(max = 4000) String answer,
            @Size(max = 40) String category) {}

    public record RoundRequest(
            @NotBlank @Size(max = 80) String roundName,
            @NotNull @Min(1) Integer roundNumber,
            @NotBlank String interviewType,
            Instant startsAt,
            Instant endsAt,
            @Size(max = 500) String location,
            @NotBlank String result,
            /** 富文本 HTML，服务端白名单清洗后存储。 */
            @Size(max = 50000) String processNotes,
            @Size(max = 50000) String reviewSummary,
            List<@Valid QuestionItem> questions,
            Boolean createSchedule,
            Long version) {}

    public record QuestionResponse(long sortOrder, String question, String answer, String category) {}

    public record RoundResponse(String id, String positionId, String positionTitle, String companyName,
            String roundName, int roundNumber, String interviewType, Instant startsAt, Instant endsAt,
            String location, String result, String processNotes, String reviewSummary,
            List<QuestionResponse> questions, List<String> scheduleIds,
            long version, Instant createdAt, Instant updatedAt) {}

    public record RoundListResponse(List<RoundResponse> items) {}
}
