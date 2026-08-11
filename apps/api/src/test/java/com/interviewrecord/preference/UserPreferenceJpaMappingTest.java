package com.interviewrecord.preference;

import static org.assertj.core.api.Assertions.assertThat;

import com.interviewrecord.preference.domain.UserPreference;
import com.interviewrecord.support.MySqlIntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserPreferenceJpaMappingTest extends MySqlIntegrationTestBase {
    @Autowired EntityManager entityManager;

    @Test
    void startsWithSingleWritableUserIdMapping() {
        assertThat(entityManager.getMetamodel().entity(UserPreference.class).getIdType().getJavaType()).isEqualTo(Long.class);
    }
}
