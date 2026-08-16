package com.interviewrecord.interviews;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** 题库接口的真实 MySQL 集成测试：跨用户隔离与过滤分页（随 TEST_DB 环境运行）。 */
class QuestionBankMysqlIntegrationTest extends MySqlIntegrationTestBase {
    @Autowired MockMvc mvc;
    @Autowired JpaUserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired UserDefaultsService userDefaultsService;

    private final ObjectMapper json = new ObjectMapper();
    private User alice;
    private User bob;

    @BeforeEach
    void createUsers() {
        Instant now = Instant.now();
        alice = users.save(new User("alice-qb@example.com", passwordEncoder.encode("Password123"), "Alice", now));
        alice.verify(now);
        users.save(alice);
        bob = users.save(new User("bob-qb@example.com", passwordEncoder.encode("Password123"), "Bob", now));
        bob.verify(now);
        users.save(bob);
        userDefaultsService.createFor(alice, "Asia/Shanghai", now);
        userDefaultsService.createFor(bob, "Asia/Shanghai", now);
    }

    private String firstId(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString()).get(0).get("id").asText();
    }

    private String createPositionWithRoundAndQuestion(User owner, String questionText, String category) throws Exception {
        String jobTypeId = firstId(mvc.perform(get("/api/v1/job-types").with(authentication(authFor(owner))))
                .andExpect(status().isOk()).andReturn());
        String statusId = firstId(mvc.perform(get("/api/v1/statuses").with(authentication(authFor(owner))))
                .andExpect(status().isOk()).andReturn());
        var createCompany = mvc.perform(post("/api/v1/companies").with(authentication(authFor(owner))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"" + owner.displayName() + " 的公司\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String companyId = json.readTree(createCompany.getResponse().getContentAsString()).get("id").asText();

        var createPosition = mvc.perform(post("/api/v1/positions").with(authentication(authFor(owner))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"companyId\":\"" + companyId + "\",\"jobTypeId\":\"" + jobTypeId
                                + "\",\"statusId\":\"" + statusId + "\",\"title\":\"后端开发工程师\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String positionId = json.readTree(createPosition.getResponse().getContentAsString()).get("id").asText();

        var createRound = mvc.perform(post("/api/v1/positions/" + positionId + "/interview-rounds")
                        .with(authentication(authFor(owner))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"roundName\":\"一面\",\"roundNumber\":1,\"interviewType\":\"VIDEO\",\"result\":\"UPCOMING\","
                                + "\"questions\":[{\"question\":\"" + questionText + "\",\"answer\":\"回答\",\"category\":\"" + category + "\"}]}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode round = json.readTree(createRound.getResponse().getContentAsString());
        return round.get("id").asText();
    }

    @Test
    void questionBankIsIsolatedBetweenUsers() throws Exception {
        createPositionWithRoundAndQuestion(alice, "动态规划是什么", "算法");

        mvc.perform(get("/api/v1/interview-rounds/questions").with(authentication(authFor(alice))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].question").value("动态规划是什么"));

        // 其他用户看不到 Alice 的题目，总数不受影响
        mvc.perform(get("/api/v1/interview-rounds/questions").with(authentication(authFor(bob))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void questionBankFiltersByCategoryAndKeyword() throws Exception {
        createPositionWithRoundAndQuestion(alice, "动态规划是什么", "算法");
        createPositionWithRoundAndQuestion(alice, "介绍一个项目", "项目");

        mvc.perform(get("/api/v1/interview-rounds/questions")
                        .with(authentication(authFor(alice))).param("category", "算法"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].question").value("动态规划是什么"));

        mvc.perform(get("/api/v1/interview-rounds/questions")
                        .with(authentication(authFor(alice))).param("keyword", "项目"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].question").value("介绍一个项目"));

        mvc.perform(get("/api/v1/interview-rounds/questions/random")
                        .with(authentication(authFor(alice))).param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    private UsernamePasswordAuthenticationToken authFor(User user) {
        return UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(user.id(), user.email(), user.displayName(), user.isVerified(),
                        "Asia/Shanghai", Theme.GRAPHITE_CORAL), null,
                java.util.List.of());
    }
}
