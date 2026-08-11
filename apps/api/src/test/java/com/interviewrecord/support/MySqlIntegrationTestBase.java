package com.interviewrecord.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class MySqlIntegrationTestBase {

    @DynamicPropertySource
    protected static void database(DynamicPropertyRegistry registry) {
        MySqlTestDatabase.configure(registry);
    }
}
