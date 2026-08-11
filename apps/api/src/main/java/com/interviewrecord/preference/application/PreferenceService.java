package com.interviewrecord.preference.application;

import com.interviewrecord.common.error.InvalidRegistrationException;
import com.interviewrecord.preference.api.PreferenceDtos;
import com.interviewrecord.preference.domain.UserPreference;
import com.interviewrecord.preference.infrastructure.JpaUserPreferenceRepository;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class PreferenceService {
    private final JpaUserPreferenceRepository preferences;
    private final Clock clock;

    public PreferenceService(JpaUserPreferenceRepository preferences, Clock clock) {
        this.preferences = preferences;
        this.clock = clock;
    }

    @Transactional
    public PreferenceDtos.PreferenceResponse update(long userId, PreferenceDtos.UpdatePreferencesRequest request) {
        validateTimeZone(request.timeZone());
        UserPreference preference = preferences.requireByUserId(userId);
        List<Integer> interviewOffsets = normalizeOffsets(request.interviewReminderOffsets());
        List<Integer> deadlineOffsets = normalizeOffsets(request.deadlineReminderOffsets());
        preference.user().changeDisplayName(request.displayName().trim(), clock.instant());
        preference.update(request.timeZone(), request.theme(), interviewOffsets, deadlineOffsets, clock.instant());
        return new PreferenceDtos.PreferenceResponse(preference.user().displayName(), preference.timeZone(), preference.theme(),
                interviewOffsets, deadlineOffsets);
    }

    private void validateTimeZone(String value) {
        try {
            ZoneId.of(value);
        } catch (RuntimeException exception) {
            throw new InvalidRegistrationException("INVALID_TIME_ZONE");
        }
    }

    private List<Integer> normalizeOffsets(List<Integer> offsets) {
        return offsets.stream().distinct().sorted(Comparator.reverseOrder()).toList();
    }
}
