package com.interviewrecord.preference.api;

import com.interviewrecord.preference.domain.Theme;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class PreferenceDtos {
    private PreferenceDtos() {
    }

    public record UpdatePreferencesRequest(
            @NotBlank @Size(max = 80) String displayName,
            @NotBlank @Size(max = 64) String timeZone,
            @NotNull Theme theme,
            @NotNull List<@NotNull @Min(0) @Max(10080) Integer> interviewReminderOffsets,
            @NotNull List<@NotNull @Min(0) @Max(10080) Integer> deadlineReminderOffsets) {
    }

    public record PreferenceResponse(String displayName, String timeZone, Theme theme,
            List<Integer> interviewReminderOffsets, List<Integer> deadlineReminderOffsets) {
    }
}
