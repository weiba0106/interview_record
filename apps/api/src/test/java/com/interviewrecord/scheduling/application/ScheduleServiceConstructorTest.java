package com.interviewrecord.scheduling.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ScheduleServiceConstructorTest {
    @Test
    void springConstructorIsExplicitlyMarkedForAutowiring() throws Exception {
        Constructor<ScheduleService> constructor = ScheduleService.class.getConstructor(
                com.interviewrecord.scheduling.infrastructure.JpaScheduleEventRepository.class,
                com.interviewrecord.tracking.infrastructure.JpaPositionRepository.class,
                com.interviewrecord.interviews.infrastructure.JpaInterviewRoundRepository.class,
                com.interviewrecord.reminders.application.ReminderService.class,
                java.time.Clock.class);

        assertThat(constructor.isAnnotationPresent(Autowired.class)).isTrue();
    }
}
