package com.interviewrecord.preference.infrastructure;

import com.interviewrecord.preference.domain.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    default UserPreference requireByUserId(long userId) { return findById(userId).orElseThrow(() -> new IllegalArgumentException("PREFERENCE_NOT_FOUND")); }
}
