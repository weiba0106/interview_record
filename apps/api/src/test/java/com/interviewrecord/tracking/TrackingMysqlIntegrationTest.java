package com.interviewrecord.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.interviewrecord.auth.domain.User;
import com.interviewrecord.auth.infrastructure.JpaUserRepository;
import com.interviewrecord.common.security.AuthenticatedUser;
import com.interviewrecord.defaults.application.UserDefaultsService;
import com.interviewrecord.preference.domain.Theme;
import com.interviewrecord.support.MySqlIntegrationTestBase;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class TrackingMysqlIntegrationTest extends MySqlIntegrationTestBase {
    @Autowired MockMvc mvc;
    @Autowired JpaUserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired UserDefaultsService userDefaultsService;
    @Autowired JdbcTemplate jdbc;

    private final ObjectMapper json = new ObjectMapper();
    private User alice;
    private User bob;

    @BeforeEach
    void createUsers() {
        Instant now = Instant.now();
        alice = users.save(new User("alice-it@example.com", passwordEncoder.encode("Password123"), "Alice", now));
        alice.verify(now);
        users.save(alice);
        bob = users.save(new User("bob-it@example.com", passwordEncoder.encode("Password123"), "Bob", now));
        bob.verify(now);
        users.save(bob);
        userDefaultsService.createFor(alice, "Asia/Shanghai", now);
        userDefaultsService.createFor(bob, "Asia/Shanghai", now);
    }

    @Test
    void companiesAreIsolatedBetweenUsers() throws Exception {
        String companyId = createCompany(alice, "字节跳动");

        mvc.perform(get("/api/v1/companies").with(authentication(authFor(alice))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("字节跳动"));
        mvc.perform(get("/api/v1/companies").with(authentication(authFor(bob))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mvc.perform(get("/api/v1/companies/" + companyId).with(authentication(authFor(bob))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mvc.perform(delete("/api/v1/companies/" + companyId).with(authentication(authFor(bob))).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(count("companies")).isEqualTo(1);
    }

    @Test
    void duplicateCompanyNameRequiresExplicitConfirmation() throws Exception {
        createCompany(alice, "腾讯");

        mvc.perform(post("/api/v1/companies").with(authentication(authFor(alice))).with(csrf())
                        .contentType(APPLICATION_JSON).content("{\"name\":\"腾讯\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMPANY_DUPLICATE"));

        mvc.perform(post("/api/v1/companies").with(authentication(authFor(alice))).with(csrf())
                        .contentType(APPLICATION_JSON).content("{\"name\":\"腾讯\",\"confirmDuplicate\":true}"))
                .andExpect(status().isCreated());
        assertThat(count("companies")).isEqualTo(2);
    }

    @Test
    void positionsBelongingToMultipleCompaniesCanBeFiltered() throws Exception {
        String companyA = createCompany(alice, "阿里巴巴");
        String companyB = createCompany(alice, "美团");
        String jobTypeId = firstId(get("/api/v1/job-types").with(authentication(authFor(alice))));
        String statusId = firstId(get("/api/v1/statuses").with(authentication(authFor(alice))));

        createPosition(alice, companyA, jobTypeId, statusId, "Java 后端");
        createPosition(alice, companyA, jobTypeId, statusId, "Go 后端");
        createPosition(alice, companyB, jobTypeId, statusId, "前端开发");

        mvc.perform(get("/api/v1/positions").with(authentication(authFor(alice))).param("companyId", companyA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2));
        mvc.perform(get("/api/v1/positions").with(authentication(authFor(alice))).param("keyword", "后端"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2));
        mvc.perform(get("/api/v1/positions").with(authentication(authFor(alice))).param("keyword", "前端"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1));

        mvc.perform(get("/api/v1/positions").with(authentication(authFor(bob))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void jobTypesAndStatusesCannotBeUpdatedAcrossUsers() throws Exception {
        String aliceJobTypeId = firstId(get("/api/v1/job-types").with(authentication(authFor(alice))));
        String aliceStatusId = firstId(get("/api/v1/statuses").with(authentication(authFor(alice))));

        mvc.perform(put("/api/v1/job-types/" + aliceJobTypeId).with(authentication(authFor(bob))).with(csrf())
                        .contentType(APPLICATION_JSON).content("{\"name\":\"窃取的类型\",\"active\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mvc.perform(put("/api/v1/statuses/" + aliceStatusId).with(authentication(authFor(bob))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"窃取的状态\",\"color\":\"#123456\","
                                + "\"statisticsCategory\":\"ACTIVE\",\"active\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void positionDeleteRequiresConfirmationAndCascadesRoundsAndSchedules() throws Exception {
        String companyId = createCompany(alice, "小米");
        String jobTypeId = firstId(get("/api/v1/job-types").with(authentication(authFor(alice))));
        String statusId = firstId(get("/api/v1/statuses").with(authentication(authFor(alice))));
        String positionId = createPosition(alice, companyId, jobTypeId, statusId, "测试开发");
        Instant startsAt = Instant.now().plus(2, ChronoUnit.DAYS);
        createRound(alice, positionId, 1, startsAt, true);

        assertThat(count("interview_rounds")).isEqualTo(1);
        assertThat(count("schedule_events")).isEqualTo(1);

        mvc.perform(delete("/api/v1/positions/" + positionId).with(authentication(authFor(alice))).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POSITION_DELETE_CONFIRM_REQUIRED"));
        assertThat(count("positions")).isEqualTo(1);

        mvc.perform(delete("/api/v1/positions/" + positionId + "?confirmed=true")
                        .with(authentication(authFor(alice))).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(count("positions")).isZero();
        assertThat(count("interview_rounds")).isZero();
        assertThat(count("schedule_events")).isZero();
    }

    @Test
    void companyDeleteWithPositionsRequiresConfirmationAndCascadesEverything() throws Exception {
        String companyId = createCompany(alice, "网易");
        String jobTypeId = firstId(get("/api/v1/job-types").with(authentication(authFor(alice))));
        String statusId = firstId(get("/api/v1/statuses").with(authentication(authFor(alice))));
        String positionId = createPosition(alice, companyId, jobTypeId, statusId, "算法工程师");
        createRound(alice, positionId, 1, Instant.now().plus(1, ChronoUnit.DAYS), false);

        mvc.perform(delete("/api/v1/companies/" + companyId).with(authentication(authFor(alice))).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMPANY_HAS_POSITIONS"));

        mvc.perform(delete("/api/v1/companies/" + companyId + "?confirmed=true")
                        .with(authentication(authFor(alice))).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(count("companies")).isZero();
        assertThat(count("positions")).isZero();
        assertThat(count("interview_rounds")).isZero();
    }

    @Test
    void roundNumbersAreUniquePerPositionAndListedInOrder() throws Exception {
        String companyId = createCompany(alice, "蚂蚁集团");
        String jobTypeId = firstId(get("/api/v1/job-types").with(authentication(authFor(alice))));
        String statusId = firstId(get("/api/v1/statuses").with(authentication(authFor(alice))));
        String positionId = createPosition(alice, companyId, jobTypeId, statusId, "风控开发");

        createRound(alice, positionId, 2, null, false);
        createRound(alice, positionId, 1, null, false);

        mvc.perform(post("/api/v1/positions/" + positionId + "/interview-rounds")
                        .with(authentication(authFor(alice))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"roundName\":\"重复一面\",\"roundNumber\":1,\"interviewType\":\"PHONE\",\"result\":\"UPCOMING\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROUND_NUMBER_TAKEN"));

        mvc.perform(get("/api/v1/positions/" + positionId + "/interview-rounds")
                        .with(authentication(authFor(alice))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].roundNumber").value(1))
                .andExpect(jsonPath("$.items[1].roundNumber").value(2));

        String otherPositionId = createPosition(alice, companyId, jobTypeId, statusId, "数据开发");
        createRound(alice, otherPositionId, 1, null, false);
        assertThat(count("interview_rounds")).isEqualTo(3);
    }

    @Test
    void updatingRoundTimeSynchronizesLinkedSchedule() throws Exception {
        String companyId = createCompany(alice, "华为");
        String jobTypeId = firstId(get("/api/v1/job-types").with(authentication(authFor(alice))));
        String statusId = firstId(get("/api/v1/statuses").with(authentication(authFor(alice))));
        String positionId = createPosition(alice, companyId, jobTypeId, statusId, "软件开发");
        Instant original = Instant.parse("2026-08-20T02:00:00Z");
        String roundId = createRound(alice, positionId, 1, original, true);

        mvc.perform(get("/api/v1/schedules").with(authentication(authFor(alice))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].startsAt").value("2026-08-20T02:00:00Z"));

        Instant moved = Instant.parse("2026-08-21T08:30:00Z");
        mvc.perform(put("/api/v1/interview-rounds/" + roundId).with(authentication(authFor(alice))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"roundName\":\"技术一面\",\"roundNumber\":1,\"interviewType\":\"VIDEO\","
                                + "\"startsAt\":\"2026-08-21T08:30:00Z\",\"endsAt\":\"2026-08-21T09:30:00Z\","
                                + "\"result\":\"UPCOMING\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/schedules").with(authentication(authFor(alice))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].startsAt").value("2026-08-21T08:30:00Z"))
                .andExpect(jsonPath("$.items[0].endsAt").value("2026-08-21T09:30:00Z"));
    }

    @Test
    void schedulesAreIsolatedBetweenUsers() throws Exception {
        Instant soon = Instant.now().plus(1, ChronoUnit.DAYS);
        MvcResult created = mvc.perform(post("/api/v1/schedules").with(authentication(authFor(alice))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"笔试\",\"eventType\":\"WRITTEN_TEST\",\"startsAt\":\"" + soon + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String scheduleId = json.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mvc.perform(get("/api/v1/schedules/" + scheduleId).with(authentication(authFor(bob))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mvc.perform(get("/api/v1/schedules").with(authentication(authFor(bob))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void dashboardMetricsCountOnlyTheOwnersData() throws Exception {
        String companyId = createCompany(alice, "京东");
        String jobTypeId = firstId(get("/api/v1/job-types").with(authentication(authFor(alice))));
        String statusesResponse = mvc.perform(get("/api/v1/statuses").with(authentication(authFor(alice))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode statusList = json.readTree(statusesResponse);
        String activeStatusId = statusList.get(0).get("id").asText();
        String offerStatusId = null;
        for (JsonNode node : statusList) {
            if ("SUCCESS".equals(node.get("statisticsCategory").asText())) {
                offerStatusId = node.get("id").asText();
            }
        }
        createPosition(alice, companyId, jobTypeId, activeStatusId, "采销");
        createPosition(alice, companyId, jobTypeId, offerStatusId, "物流产品");
        Instant soon = Instant.now().plus(2, ChronoUnit.DAYS);
        mvc.perform(post("/api/v1/schedules").with(authentication(authFor(alice))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"HR 沟通\",\"eventType\":\"HR_COMMUNICATION\",\"startsAt\":\"" + soon + "\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/dashboard").with(authentication(authFor(alice))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics.totalPositions").value(2))
                .andExpect(jsonPath("$.metrics.activePositions").value(1))
                .andExpect(jsonPath("$.metrics.offerCount").value(1))
                .andExpect(jsonPath("$.metrics.upcomingScheduleCount").value(1))
                .andExpect(jsonPath("$.positions.length()").value(2))
                .andExpect(jsonPath("$.schedules.length()").value(1));

        mvc.perform(get("/api/v1/dashboard").with(authentication(authFor(bob))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics.totalPositions").value(0))
                .andExpect(jsonPath("$.positions").isEmpty())
                .andExpect(jsonPath("$.schedules").isEmpty());
    }

    @Test
    void positionCreateCanQuickCreateANewCompanyInline() throws Exception {
        String jobTypeId = firstId(get("/api/v1/job-types").with(authentication(authFor(alice))));
        String statusId = firstId(get("/api/v1/statuses").with(authentication(authFor(alice))));

        MvcResult created = mvc.perform(post("/api/v1/positions").with(authentication(authFor(alice))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"newCompanyName\":\"新力科技\",\"jobTypeId\":\"" + jobTypeId
                                + "\",\"statusId\":\"" + statusId + "\",\"title\":\"后端开发\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName").value("新力科技"))
                .andReturn();
        assertThat(count("companies")).isEqualTo(1);
        assertThat(count("positions")).isEqualTo(1);

        // 同名快速新建复用已有公司，不产生重复公司
        mvc.perform(post("/api/v1/positions").with(authentication(authFor(alice))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"newCompanyName\":\"新力科技\",\"jobTypeId\":\"" + jobTypeId
                                + "\",\"statusId\":\"" + statusId + "\",\"title\":\"前端开发\"}"))
                .andExpect(status().isCreated());
        assertThat(count("companies")).isEqualTo(1);
        assertThat(count("positions")).isEqualTo(2);

        // 其他用户输入同名公司时创建自己的公司，不复用别人的
        mvc.perform(post("/api/v1/positions").with(authentication(authFor(bob))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"newCompanyName\":\"新力科技\",\"jobTypeId\":\"" + firstId(get("/api/v1/job-types").with(authentication(authFor(bob))))
                                + "\",\"statusId\":\"" + firstId(get("/api/v1/statuses").with(authentication(authFor(bob))))
                                + "\",\"title\":\"测试岗\"}"))
                .andExpect(status().isCreated());
        assertThat(count("companies")).isEqualTo(2);
        String positionId = json.readTree(created.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(get("/api/v1/positions/" + positionId).with(authentication(authFor(bob))))
                .andExpect(status().isNotFound());
    }

    @Test
    void positionCreateWithoutCompanyReferenceIsRejected() throws Exception {
        String jobTypeId = firstId(get("/api/v1/job-types").with(authentication(authFor(alice))));
        String statusId = firstId(get("/api/v1/statuses").with(authentication(authFor(alice))));

        mvc.perform(post("/api/v1/positions").with(authentication(authFor(alice))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"jobTypeId\":\"" + jobTypeId + "\",\"statusId\":\"" + statusId
                                + "\",\"title\":\"后端开发\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMPANY_REQUIRED"));
        assertThat(count("positions")).isZero();
    }

    // ---- helpers ----

    private String createCompany(User user, String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/companies").with(authentication(authFor(user))).with(csrf())
                        .contentType(APPLICATION_JSON).content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createPosition(User user, String companyId, String jobTypeId, String statusId, String title)
            throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/positions").with(authentication(authFor(user))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"companyId\":\"" + companyId + "\",\"jobTypeId\":\"" + jobTypeId
                                + "\",\"statusId\":\"" + statusId + "\",\"title\":\"" + title + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createRound(User user, String positionId, int roundNumber, Instant startsAt,
            boolean createSchedule) throws Exception {
        String time = startsAt == null ? "" : "\"startsAt\":\"" + startsAt.toString().replace("+00:00", "Z")
                + "\",\"endsAt\":\"" + startsAt.plus(1, ChronoUnit.HOURS).toString().replace("+00:00", "Z") + "\",";
        MvcResult result = mvc.perform(post("/api/v1/positions/" + positionId + "/interview-rounds")
                        .with(authentication(authFor(user))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"roundName\":\"第" + roundNumber + "轮\",\"roundNumber\":" + roundNumber
                                + ",\"interviewType\":\"VIDEO\"," + time + "\"result\":\"UPCOMING\","
                                + "\"createSchedule\":" + createSchedule + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String firstId(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        MvcResult result = mvc.perform(request).andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get(0).get("id").asText();
    }

    private long count(String table) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return total == null ? 0 : total;
    }

    private UsernamePasswordAuthenticationToken authFor(User user) {
        AuthenticatedUser authenticated = new AuthenticatedUser(user.id(), user.email(), user.displayName(),
                true, "Asia/Shanghai", Theme.GRAPHITE_CORAL);
        return UsernamePasswordAuthenticationToken.authenticated(authenticated, null, authenticated.authorities());
    }
}
