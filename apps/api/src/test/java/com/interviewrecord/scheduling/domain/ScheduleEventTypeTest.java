package com.interviewrecord.scheduling.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ScheduleEventTypeTest {
    @Test
    void supportedTypesMatchTheSixPrdScheduleTypes() {
        assertThat(ScheduleEvent.EVENT_TYPES).containsExactlyInAnyOrder(
                "APPLY_DEADLINE", "WRITTEN_TEST", "INTERVIEW", "HR_COMMUNICATION",
                "OFFER_DEADLINE", "CUSTOM");
    }
}
