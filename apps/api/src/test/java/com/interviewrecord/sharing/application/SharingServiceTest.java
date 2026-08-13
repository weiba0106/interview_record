package com.interviewrecord.sharing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.interviewrecord.auth.application.RateLimitService;
import com.interviewrecord.common.error.NotFoundException;
import com.interviewrecord.common.token.SecureTokenService;
import com.interviewrecord.interviews.domain.InterviewQuestion;
import com.interviewrecord.interviews.domain.InterviewRound;
import com.interviewrecord.interviews.infrastructure.JpaInterviewQuestionRepository;
import com.interviewrecord.interviews.infrastructure.JpaInterviewRoundRepository;
import com.interviewrecord.sharing.api.SharingDtos;
import com.interviewrecord.sharing.domain.ShareLink;
import com.interviewrecord.sharing.domain.ShareRound;
import com.interviewrecord.sharing.infrastructure.JpaShareLinkRepository;
import com.interviewrecord.sharing.infrastructure.JpaShareRoundRepository;
import com.interviewrecord.tracking.domain.Company;
import com.interviewrecord.tracking.domain.JobType;
import com.interviewrecord.tracking.domain.Position;
import com.interviewrecord.tracking.domain.PositionStatus;
import com.interviewrecord.tracking.infrastructure.JpaCompanyRepository;
import com.interviewrecord.tracking.infrastructure.JpaManagedJobTypeRepository;
import com.interviewrecord.tracking.infrastructure.JpaManagedPositionStatusRepository;
import com.interviewrecord.tracking.infrastructure.JpaPositionRepository;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SharingServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-13T00:00:00Z");
    private static final long ALICE = 11L;

    @Mock JpaShareLinkRepository links;
    @Mock JpaShareRoundRepository shareRounds;
    @Mock JpaPositionRepository positions;
    @Mock JpaCompanyRepository companies;
    @Mock JpaManagedJobTypeRepository jobTypes;
    @Mock JpaManagedPositionStatusRepository statuses;
    @Mock JpaInterviewRoundRepository rounds;
    @Mock JpaInterviewQuestionRepository questions;
    @Mock RateLimitService rateLimits;
    @Captor ArgumentCaptor<ShareLink> savedLink;
    private SharingService service;

    @BeforeEach
    void setUp() {
        service = new SharingService(links, shareRounds, positions, companies, jobTypes, statuses, rounds, questions,
                new SecureTokenService(Clock.fixed(NOW, ZoneOffset.UTC)), rateLimits, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void publicViewOnlyContainsThePersistedFieldAndRoundAllowlists() throws Exception {
        Position position = position(101L);
        InterviewRound round = round(201L, position.id());
        ShareLink link = link(301L, position.id(), Set.of("COMPANY_NAME", "POSITION_TITLE"), null, null);
        ShareRound selection = shareRound(link.id(), round.id(), Set.of("BASIC_INFO", "QUESTIONS"));
        given(links.findByTokenHash(any())).willReturn(java.util.Optional.of(link));
        given(positions.findByIdAndUserId(position.id(), ALICE)).willReturn(java.util.Optional.of(position));
        given(companies.findByIdAndUserId(position.companyId(), ALICE)).willReturn(java.util.Optional.of(company()));
        given(rounds.findByIdAndUserId(round.id(), ALICE)).willReturn(java.util.Optional.of(round));
        given(shareRounds.findAllByShareIdOrderByIdAsc(link.id())).willReturn(List.of(selection));
        given(questions.findAllByUserIdAndRoundIdOrderBySortOrderAsc(ALICE, round.id()))
                .willReturn(List.of(new InterviewQuestion(ALICE, round.id(), 1, "TCP 握手", "不应公开的回答", "网络", NOW)));

        SharingDtos.PublicShareResponse response = service.getPublic("valid-share-token", "203.0.113.8");

        assertThat(response.position()).containsOnlyKeys("companyName", "positionTitle");
        assertThat(response.position()).doesNotContainKeys("applyUrl", "description", "status", "jobType");
        assertThat(response.rounds()).singleElement().satisfies(sharedRound -> {
            assertThat(sharedRound.content()).containsKeys("basicInfo", "questions");
            assertThat(sharedRound.content()).doesNotContainKeys("answers", "processNotes", "reviewSummary", "result");
            assertThat(sharedRound.content().get("questions").toString()).doesNotContain("不应公开的回答");
        });
    }

    @Test
    void revokedExpiredOrDeletedPositionLinksAllAppearInvalid() throws Exception {
        ShareLink revoked = link(301L, 101L, Set.of("POSITION_TITLE"), NOW.plusSeconds(3600), NOW.minusSeconds(1));
        given(links.findByTokenHash(any())).willReturn(java.util.Optional.of(revoked));
        assertThatThrownBy(() -> service.getPublic("revoked", "203.0.113.8")).isInstanceOf(NotFoundException.class);

        ShareLink expired = link(302L, 101L, Set.of("POSITION_TITLE"), NOW.minusSeconds(1), null);
        given(links.findByTokenHash(any())).willReturn(java.util.Optional.of(expired));
        assertThatThrownBy(() -> service.getPublic("expired", "203.0.113.8")).isInstanceOf(NotFoundException.class);

        ShareLink deletedPosition = link(303L, 101L, Set.of("POSITION_TITLE"), null, null);
        given(links.findByTokenHash(any())).willReturn(java.util.Optional.of(deletedPosition));
        given(positions.findByIdAndUserId(101L, ALICE)).willReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> service.getPublic("deleted", "203.0.113.8")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void createStoresOnlyATokenHashAndRejectsAnotherUsersRound() throws Exception {
        Position position = position(101L);
        given(positions.findByIdAndUserId(position.id(), ALICE)).willReturn(java.util.Optional.of(position));
        given(links.save(any())).willAnswer(invocation -> {
            ShareLink link = invocation.getArgument(0);
            setId(link, 301L);
            return link;
        });
        SharingDtos.CreateShareRequest request = new SharingDtos.CreateShareRequest(
                Set.of("POSITION_TITLE"), List.of(), "SEVEN_DAYS");

        SharingDtos.CreatedShareResponse response = service.create(ALICE, position.id(), request);

        org.mockito.Mockito.verify(links).save(savedLink.capture());
        assertThat(response.token()).isNotBlank();
        assertThat(savedLink.getValue().tokenHash()).hasSize(32);
        assertThat(savedLink.getValue().tokenHash()).isNotEqualTo(response.token().getBytes(java.nio.charset.StandardCharsets.UTF_8));

        given(rounds.findByIdAndUserId(999L, ALICE)).willReturn(java.util.Optional.empty());
        SharingDtos.CreateShareRequest foreignRound = new SharingDtos.CreateShareRequest(Set.of(),
                List.of(new SharingDtos.RoundSelection("999", Set.of("BASIC_INFO"))), "ONE_DAY");
        assertThatThrownBy(() -> service.create(ALICE, position.id(), foreignRound)).isInstanceOf(NotFoundException.class);
    }

    private Position position(Long id) throws Exception {
        Position value = new Position(ALICE, 51L, 61L, 71L, "Backend Intern", "https://private.example/apply",
                null, null, "Shanghai", "private notes", NOW);
        setId(value, id);
        return value;
    }

    private Company company() throws Exception {
        Company value = new Company(ALICE, "Acme", null, null, NOW);
        setId(value, 51L);
        return value;
    }

    private InterviewRound round(Long id, Long positionId) throws Exception {
        InterviewRound value = new InterviewRound(ALICE, positionId, "Technical", 1, "VIDEO", NOW, null,
                "Tencent Meeting", "PASSED", "private process", "private review", NOW);
        setId(value, id);
        return value;
    }

    private ShareLink link(Long id, Long positionId, Set<String> positionFields, Instant expiresAt, Instant revokedAt)
            throws Exception {
        ShareLink value = new ShareLink(ALICE, positionId, new byte[32], positionFields, expiresAt, NOW);
        setId(value, id);
        if (revokedAt != null) value.revoke(revokedAt);
        return value;
    }

    private ShareRound shareRound(Long shareId, Long roundId, Set<String> fields) throws Exception {
        ShareRound value = new ShareRound(shareId, roundId, fields, NOW);
        setId(value, 401L);
        return value;
    }

    private static void setId(Object target, Long id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
