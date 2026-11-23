package com.example.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.app.character.GameCharacter;
import com.example.app.character.GameCharacterRepository;
import com.example.app.common.CurrentUserProvider;
import com.example.app.intake.DailySummary;
import com.example.app.intake.DailySummaryRepository;
import com.example.app.intake.IntakeLog;
import com.example.app.intake.IntakeLogRepository;
import com.example.app.reward.RewardLedger;
import com.example.app.reward.RewardLedgerRepository;
import com.example.app.user.OAuthProvider;
import com.example.app.user.User;
import com.example.app.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RewardApiTests {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 23);

    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired UserRepository users;
    @Autowired IntakeLogRepository logs;
    @Autowired DailySummaryRepository summaries;
    @Autowired GameCharacterRepository characters;
    @Autowired RewardLedgerRepository ledger;
    @MockitoBean Clock clock;
    @MockitoBean CurrentUserProvider currentUser;

    Long userId;

    @BeforeEach
    void setUp() {
        userId = users.save(new User(OAuthProvider.KAKAO, UUID.randomUUID().toString(), "테스트")).getId();
        given(currentUser.currentUserId()).willReturn(Optional.of(userId));
        koreanTime("2026-11-23T08:00");
    }

    @Test
    void createsOnlyOneActiveCharacter() throws Exception {
        call(get("/api/v1/characters/active")).andExpect(jsonPath("$.data.character").isEmpty())
                .andExpect(jsonPath("$.data.streak.current").value(0));
        startCharacter().andExpect(status().isCreated()).andExpect(jsonPath("$.data.nextLevelExp").value(100));
        startCharacter().andExpect(status().isConflict());
        call(get("/api/v1/characters/active")).andExpect(jsonPath("$.data.character.name").value("냥이"));
        call(post("/api/v1/characters"), "{\"speciesCode\":\"DRAGON\",\"name\":\"a\"}").andExpect(status().isBadRequest());
        call(post("/api/v1/characters"), "{\"speciesCode\":\"CAT\",\"name\":\" \"}").andExpect(status().isBadRequest());
    }

    @Test
    void rewardsByTimingAndCompletesDay() throws Exception {
        startCharacter();
        List<Long> ids = register("08:30", "20:00");
        koreanTime("2026-11-23T08:40");
        take(ids.get(0)).andExpect(jsonPath("$.data.results[0].exp").value(10))
                .andExpect(jsonPath("$.data.bonuses.length()").value(0))
                .andExpect(jsonPath("$.data.streak.current").value(0));
        koreanTime("2026-11-23T21:00");
        take(ids.get(1)).andExpect(jsonPath("$.data.results[0].exp").value(5))
                .andExpect(jsonPath("$.data.bonuses[0].type").value("DAY_COMPLETE"))
                .andExpect(jsonPath("$.data.character.exp").value(35))
                .andExpect(jsonPath("$.data.streak.current").value(1));
        take(ids.get(1)).andExpect(jsonPath("$.data.results[0].exp").value(0))
                .andExpect(jsonPath("$.data.character.exp").value(35));
        assertThat(ledgerSum()).isEqualTo(35);
    }

    @Test
    void cancelReclaimsAndRetakeGrantsAgain() throws Exception {
        startCharacter();
        long id = register("08:30").get(0);
        for (int i = 0; i < 3; i++) {
            take(id).andExpect(jsonPath("$.data.character.exp").value(30))
                    .andExpect(jsonPath("$.data.streak.current").value(1));
            cancel(id).andExpect(jsonPath("$.data.reclaimedExp").value(30))
                    .andExpect(jsonPath("$.data.character.exp").value(0))
                    .andExpect(jsonPath("$.data.streak.current").value(0));
            cancel(id).andExpect(jsonPath("$.data.reclaimedExp").value(0));
        }
        assertThat(ledgerSum()).isZero();
    }

    @Test
    void cancelNeverLowersLevelAndCannotBeExploited() throws Exception {
        startCharacter();
        long id = register("08:30").get(0);
        GameCharacter c = active();
        c.gain(95, Instant.now());
        take(id).andExpect(jsonPath("$.data.character.level").value(2))
                .andExpect(jsonPath("$.data.character.exp").value(25))
                .andExpect(jsonPath("$.data.character.leveledUp").value(true));
        for (int i = 0; i < 3; i++) {
            cancel(id).andExpect(jsonPath("$.data.reclaimedExp").value(25))
                    .andExpect(jsonPath("$.data.character.level").value(2))
                    .andExpect(jsonPath("$.data.character.exp").value(0));
            take(id).andExpect(jsonPath("$.data.character.exp").value(25));
        }
    }

    @Test
    void capsDailyIntakeExp() throws Exception {
        startCharacter();
        String[] times = IntStream.range(0, 12).mapToObj(i -> "08:%02d".formatted(10 + i)).toArray(String[]::new);
        List<Long> ids = register(times);
        call(post("/api/v1/intakes/take"), "{\"intakeIds\":" + ids + "}")
                .andExpect(jsonPath("$.data.results[9].exp").value(10))
                .andExpect(jsonPath("$.data.results[10].exp").value(0))
                .andExpect(jsonPath("$.data.character.level").value(2))
                .andExpect(jsonPath("$.data.character.exp").value(20));
        assertThat(ledgerSum()).isEqualTo(120);
    }

    @Test
    void grantsStreakBonusOnceAndReclaimsOnCancel() throws Exception {
        startCharacter();
        for (int i = 1; i <= 6; i++) {
            summary(TODAY.minusDays(i), 5, 4);
        }
        summary(TODAY.minusDays(7), 5, 3);
        long id = register("08:30").get(0);
        call(get("/api/v1/characters/active")).andExpect(jsonPath("$.data.streak.current").value(6));
        take(id).andExpect(jsonPath("$.data.streak.current").value(7))
                .andExpect(jsonPath("$.data.bonuses[1].type").value("STREAK"))
                .andExpect(jsonPath("$.data.bonuses[1].exp").value(100))
                .andExpect(jsonPath("$.data.character.level").value(2));
        // 130 중 레벨 2의 30까지만 회수되고, 다시 체크해도 총합은 130을 넘지 않는다.
        cancel(id).andExpect(jsonPath("$.data.reclaimedExp").value(30))
                .andExpect(jsonPath("$.data.streak.current").value(6));
        take(id).andExpect(jsonPath("$.data.character.exp").value(30));
        assertThat(ledgerSum()).isEqualTo(130);
    }

    @Test
    void streakSkipsDaysWithoutScheduleAndBreaksOnFailure() throws Exception {
        summary(TODAY.minusDays(1), 2, 2);
        summary(TODAY.minusDays(2), 0, 0);
        summary(TODAY.minusDays(4), 5, 4);
        summary(TODAY.minusDays(5), 5, 3);
        summary(TODAY.minusDays(6), 1, 1);
        call(get("/api/v1/characters/active")).andExpect(jsonPath("$.data.streak.current").value(2));
        summary(TODAY.plusDays(1), 1, 0);
        call(get("/api/v1/characters/active")).andExpect(jsonPath("$.data.streak.current").value(2));
        given(clock.instant()).willReturn(Instant.parse("2026-11-25T00:00:00Z"));
        call(get("/api/v1/characters/active")).andExpect(jsonPath("$.data.streak.current").value(0));
    }

    @Test
    void retroCheckGivesSmallRewardAndRestoresStreak() throws Exception {
        startCharacter();
        long id = register("23:50").get(0);
        koreanTime("2026-11-24T00:10"); // 20분 뒤지만 날짜가 바뀌었으므로 소급
        take(id).andExpect(jsonPath("$.data.results[0].status").value("LATE"))
                .andExpect(jsonPath("$.data.results[0].exp").value(2))
                .andExpect(jsonPath("$.data.bonuses.length()").value(0))
                .andExpect(jsonPath("$.data.streak.current").value(1));
    }

    @Test
    void deletingSupplementKeepsPastStreak() throws Exception {
        long id = register("08:30").get(0);
        Long supplementId = logs.findById(id).orElseThrow().getUserSupplementId();
        take(id);
        koreanTime("2026-11-24T07:00");
        call(delete("/api/v1/supplements/" + supplementId)).andExpect(status().isNoContent());
        call(get("/api/v1/characters/active")).andExpect(jsonPath("$.data.streak.current").value(1));
    }

    @Test
    void completesCharacterAndAllowsNext() throws Exception {
        startCharacter();
        active().gain(10_449, Instant.now());
        List<Long> ids = register("08:10", "08:20");
        assertThat(active().getLevel()).isEqualTo(19);
        take(ids.get(0)).andExpect(jsonPath("$.data.character.completed").value(true))
                .andExpect(jsonPath("$.data.character.level").value(20))
                .andExpect(jsonPath("$.data.character.stage").value(3));
        take(ids.get(1)).andExpect(jsonPath("$.data.results[0].exp").value(0))
                .andExpect(jsonPath("$.data.character").isEmpty());
        cancel(ids.get(0)).andExpect(jsonPath("$.data.reclaimedExp").value(0));
        startCharacter().andExpect(status().isCreated());
        take(ids.get(0)).andExpect(jsonPath("$.data.results[0].exp").value(0));
    }

    @Test
    void listsRewardsNewestFirst() throws Exception {
        startCharacter();
        List<Long> ids = register("08:10", "08:20", "21:00");
        take(ids.get(0));
        take(ids.get(1));
        String cursor = JsonPath.read(call(get("/api/v1/rewards").param("size", "1"))
                .andExpect(jsonPath("$.data[0].type").value("INTAKE"))
                .andReturn().getResponse().getContentAsString(), "$.page.nextCursor");
        call(get("/api/v1/rewards").param("cursor", cursor)).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.page.hasNext").value(false));
        call(get("/api/v1/rewards").param("size", "0")).andExpect(status().isBadRequest());
    }

    private ResultActions startCharacter() throws Exception {
        return call(post("/api/v1/characters"), "{\"speciesCode\":\"CAT\",\"name\":\"냥이\"}");
    }

    /** 오늘 생성된 복용 일정 id를 시각 순으로 돌려준다. */
    private List<Long> register(String... times) throws Exception {
        String json = call(post("/api/v1/supplements"), """
                {"newProduct":{"name":"비타민D"},"dosePerIntake":1,"times":[%s],
                 "daysOfWeek":["MON","TUE","WED","THU","FRI","SAT","SUN"]}
                """.formatted(List.of(times).stream().map(t -> "\"" + t + "\"").collect(Collectors.joining(","))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(json, "$.data.id")).longValue();
        return logs.findByUserSupplementId(id).stream()
                .filter(l -> l.getScheduledAt().isBefore(Instant.parse("2026-11-23T15:00:00Z")))
                .sorted(Comparator.comparing(IntakeLog::getScheduledAt)).map(IntakeLog::getId).toList();
    }

    private void summary(LocalDate date, int scheduled, int taken) {
        DailySummary summary = new DailySummary(userId, date);
        summary.update(scheduled, taken, taken);
        summaries.save(summary);
    }

    private GameCharacter active() {
        return characters.findByUserIdAndActiveMarker(userId, 1).orElseThrow();
    }

    private int ledgerSum() {
        em.flush();
        return ledger.findAll().stream().filter(r -> r.getUserId().equals(userId)).mapToInt(RewardLedger::getExp).sum();
    }

    private ResultActions take(long id) throws Exception {
        return call(post("/api/v1/intakes/take"), "{\"intakeIds\":[" + id + "]}");
    }

    private ResultActions cancel(long id) throws Exception {
        return call(post("/api/v1/intakes/" + id + "/cancel"));
    }

    private ResultActions call(MockHttpServletRequestBuilder request) throws Exception {
        em.flush();
        em.clear();
        return mvc.perform(request);
    }

    private ResultActions call(MockHttpServletRequestBuilder request, String body) throws Exception {
        return call(request.contentType(MediaType.APPLICATION_JSON).characterEncoding("UTF-8").content(body));
    }

    private void koreanTime(String local) {
        given(clock.instant()).willReturn(LocalDateTime.parse(local).toInstant(ZoneOffset.ofHours(9)));
    }
}
