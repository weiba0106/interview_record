package com.interviewrecord.defaults.application;

import com.interviewrecord.auth.domain.User;
import com.interviewrecord.defaults.domain.DefaultJobType;
import com.interviewrecord.defaults.domain.DefaultPositionStatus;
import com.interviewrecord.defaults.infrastructure.JpaJobTypeRepository;
import com.interviewrecord.defaults.infrastructure.JpaPositionStatusRepository;
import com.interviewrecord.preference.domain.UserPreference;
import com.interviewrecord.preference.infrastructure.JpaUserPreferenceRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class UserDefaultsService {
    private static final String FALLBACK_TIME_ZONE = "Asia/Shanghai";
    private final JpaUserPreferenceRepository preferences;
    private final JpaJobTypeRepository jobTypes;
    private final JpaPositionStatusRepository statuses;

    public UserDefaultsService(JpaUserPreferenceRepository preferences, JpaJobTypeRepository jobTypes,
            JpaPositionStatusRepository statuses) {
        this.preferences = preferences; this.jobTypes = jobTypes; this.statuses = statuses;
    }

    public void createFor(User user, String requestedTimeZone, Instant now) {
        preferences.save(new UserPreference(user, validTimeZoneOrFallback(requestedTimeZone), now));
        jobTypes.saveAll(List.of(new DefaultJobType(user, "秋招", now), new DefaultJobType(user, "日常实习", now)));
        statuses.saveAll(List.of(
                status(user, 1, "待投递", "#6B7280", "ACTIVE", now),
                status(user, 2, "已投递", "#3B82F6", "ACTIVE", now),
                status(user, 3, "简历筛选中", "#8B5CF6", "ACTIVE", now),
                status(user, 4, "笔试/测评中", "#F59E0B", "ACTIVE", now),
                status(user, 5, "面试中", "#E15F55", "ACTIVE", now),
                status(user, 6, "Offer", "#10B981", "SUCCESS", now),
                status(user, 7, "未通过", "#EF4444", "REJECTED", now),
                status(user, 8, "已放弃", "#9CA3AF", "WITHDRAWN", now)));
    }

    private String validTimeZoneOrFallback(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) return FALLBACK_TIME_ZONE;
        try { return ZoneId.of(timeZone.trim()).getId(); } catch (RuntimeException ignored) { return FALLBACK_TIME_ZONE; }
    }

    private DefaultPositionStatus status(User user, int order, String name, String color, String category, Instant now) {
        return new DefaultPositionStatus(user, order, name, color, category, now);
    }
}
