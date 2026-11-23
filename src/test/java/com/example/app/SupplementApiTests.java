package com.example.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.app.common.CurrentUserProvider;
import com.example.app.intake.DailySummary;
import com.example.app.intake.DailySummaryId;
import com.example.app.intake.DailySummaryRepository;
import com.example.app.intake.IntakeLog;
import com.example.app.intake.IntakeLogRepository;
import com.example.app.intake.IntakePlanner;
import com.example.app.intake.IntakeStatus;
import com.example.app.user.OAuthProvider;
import com.example.app.user.User;
import com.example.app.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
class SupplementApiTests {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 23); // 월요일

    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired UserRepository users;
    @Autowired IntakeLogRepository logs;
    @Autowired DailySummaryRepository summaries;
    @Autowired IntakePlanner planner;
    @MockitoBean Clock clock;
    @MockitoBean CurrentUserProvider currentUser;

    Long userId;

    @BeforeEach
    void setUp() {
        userId = newUser();
        loginAs(userId);
        koreanTime("2026-11-23T08:00");
    }

    @Test
    void registersAndShowsTodaysRemainingSlotsOnly() throws Exception {
        koreanTime("2026-11-23T12:00");
        long id = register("비타민D", "\"08:30\",\"20:00\"", ALL_DAYS);

        call(get("/api/v1/intakes?date=2026-11-23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.progress.scheduled").value(1))
                .andExpect(jsonPath("$.data.slots[0].time").value("20:00"))
                .andExpect(jsonPath("$.data.slots[0].items[0].supplementId").value(id))
                .andExpect(jsonPath("$.data.slots[0].items[0].status").value("PENDING"));
        call(get("/api/v1/intakes?date=2026-11-24")).andExpect(jsonPath("$.data.progress.scheduled").value(2));
        call(get("/api/v1/supplements/" + id))
                .andExpect(jsonPath("$.data.times[0]").value("08:30"))
                .andExpect(jsonPath("$.data.daysOfWeek.length()").value(7));
    }

    @Test
    void planningIsRepeatableAndFollowsDaysOfWeek() throws Exception {
        long id = register("비타민D", "\"08:30\"", "\"TUE\"");
        assertThat(logs.findByUserSupplementId(id)).hasSize(1);
        planner.planUser(userId);
        assertThat(logs.findByUserSupplementId(id)).hasSize(1);
        assertThat(logs.findByUserSupplementId(id).get(0).getScheduledAt())
                .isEqualTo(Instant.parse("2026-11-23T23:30:00Z"));
    }

    @Test
    void reportsDuplicateNutrients() throws Exception {
        register("비타민D", "\"08:30\"", ALL_DAYS);
        create("다른 비타민D", "\"09:00\"", ALL_DAYS).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.duplicateNutrients[0]").value("VIT_D"));
    }

    @Test
    void takeIsIdempotentAndCancelRestoresQuantity() throws Exception {
        long id = register("비타민D", "\"08:30\",\"20:00\"", ALL_DAYS);
        long morning = firstLog(id);
        koreanTime("2026-11-23T08:41");

        take(morning).andExpect(jsonPath("$.data.results[0].status").value("TAKEN"))
                .andExpect(jsonPath("$.data.dayCompleted").value(false));
        take(morning).andExpect(jsonPath("$.data.results[0].status").value("ALREADY_PROCESSED"));
        call(get("/api/v1/supplements/" + id)).andExpect(jsonPath("$.data.remainingQty").value(88.0));
        call(get("/api/v1/intakes")).andExpect(
                jsonPath("$.data.slots[0].items[0].takenAt").value("2026-11-23T08:41:00+09:00"));
        assertThat(summary().getTakenCount()).isEqualTo(1);

        // 체크 뒤 복용량을 바꿔도 실제로 차감한 만큼만 되돌린다.
        call(patch("/api/v1/supplements/" + id), "{\"dosePerIntake\":5}").andExpect(status().isOk());
        for (int i = 0; i < 2; i++) {
            call(post("/api/v1/intakes/" + morning + "/cancel")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }
        call(get("/api/v1/supplements/" + id)).andExpect(jsonPath("$.data.remainingQty").value(90.0));
        em.flush();
        assertThat(summary().getTakenCount()).isZero();
        assertThat(logs.findById(morning).orElseThrow().getCancelCount()).isEqualTo(1);
    }

    @Test
    void judgesLateAndCompletesTheDay() throws Exception {
        long id = register("비타민D", "\"08:30\",\"20:00\"", ALL_DAYS);
        List<Long> ids = logs.findByUserSupplementId(id).stream().map(IntakeLog::getId).sorted().toList();
        koreanTime("2026-11-23T20:31");
        List<Long> today = todayLogs(id);
        take(today.get(0)).andExpect(jsonPath("$.data.results[0].status").value("LATE"));
        take(today.get(1)).andExpect(jsonPath("$.data.results[0].status").value("LATE"))
                .andExpect(jsonPath("$.data.dayCompleted").value(true));
        assertThat(ids).hasSize(4);
    }

    @Test
    void rejectsFutureAndOldAndAllowsYesterday() throws Exception {
        long id = register("비타민D", "\"08:30\"", ALL_DAYS);
        long today = todayLogs(id).get(0);
        long tomorrow = logs.findByUserSupplementId(id).stream().map(IntakeLog::getId)
                .filter(l -> l != today).findFirst().orElseThrow();
        take(tomorrow).andExpect(status().isBadRequest());

        koreanTime("2026-11-24T07:00");
        take(today).andExpect(jsonPath("$.data.results[0].status").value("LATE"));
        call(post("/api/v1/intakes/" + today + "/cancel")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CANCEL_NOT_ALLOWED"));

        koreanTime("2026-11-26T07:00");
        take(tomorrow).andExpect(status().isBadRequest());
        planner.planUser(userId);
        assertThat(logs.findById(tomorrow).orElseThrow().getStatus()).isEqualTo(IntakeStatus.MISSED);
    }

    @Test
    void skipsAndRejectsSkippingTaken() throws Exception {
        long id = register("비타민D", "\"08:30\",\"20:00\"", ALL_DAYS);
        List<Long> today = todayLogs(id);
        call(post("/api/v1/intakes/" + today.get(0) + "/skip")).andExpect(jsonPath("$.data.status").value("SKIPPED"));
        take(today.get(1)).andExpect(status().isOk());
        call(post("/api/v1/intakes/" + today.get(1) + "/skip")).andExpect(status().isConflict());
        call(post("/api/v1/intakes/" + today.get(0) + "/cancel")).andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void changingTimesAndStatusRebuildsFutureOnly() throws Exception {
        long id = register("비타민D", "\"08:30\",\"20:00\"", ALL_DAYS);
        take(todayLogs(id).get(0)).andExpect(status().isOk());

        call(patch("/api/v1/supplements/" + id), "{\"times\":[\"08:30\",\"21:00\"]}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.times[1]").value("21:00"));
        assertThat(logs.findByUserSupplementId(id)).hasSize(4);
        call(patch("/api/v1/supplements/" + id + "/status"), "{\"status\":\"PAUSED\"}").andExpect(status().isOk());
        assertThat(logs.findByUserSupplementId(id)).hasSize(1);
        call(patch("/api/v1/supplements/" + id + "/status"), "{\"status\":\"ACTIVE\"}").andExpect(status().isOk());
        assertThat(logs.findByUserSupplementId(id)).hasSize(4);
    }

    @Test
    void deleteRemovesLogsAndSummary() throws Exception {
        long id = register("비타민D", "\"08:30\"", ALL_DAYS);
        call(delete("/api/v1/supplements/" + id)).andExpect(status().isNoContent());
        assertThat(logs.findByUserSupplementId(id)).isEmpty();
        assertThat(summary().getScheduledCount()).isZero();
        call(get("/api/v1/supplements/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void blocksOtherUsers() throws Exception {
        long id = register("비타민D", "\"08:30\"", ALL_DAYS);
        long log = firstLog(id);
        Number productId = JsonPath.read(call(get("/api/v1/supplements/" + id)).andReturn()
                .getResponse().getContentAsString(), "$.data.productId");
        loginAs(newUser());
        call(get("/api/v1/supplements/" + id)).andExpect(status().isForbidden());
        call(delete("/api/v1/supplements/" + id)).andExpect(status().isForbidden());
        take(log).andExpect(status().isForbidden());
        call(post("/api/v1/intakes/" + log + "/cancel")).andExpect(status().isForbidden());
        call(get("/api/v1/products/" + productId)).andExpect(status().isForbidden());
        call(get("/api/v1/products").param("q", "비타민D")).andExpect(jsonPath("$.data.length()").value(0));

        given(currentUser.currentUserId()).willReturn(Optional.empty());
        call(get("/api/v1/supplements")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void searchesProductsWithCursor() throws Exception {
        for (String name : List.of("마그A", "마그B", "마그%C")) {
            register(name, "\"08:30\"", ALL_DAYS);
        }
        String cursor = JsonPath.read(call(get("/api/v1/products").param("q", "마그").param("size", "2"))
                .andExpect(jsonPath("$.page.hasNext").value(true))
                .andReturn().getResponse().getContentAsString(), "$.page.nextCursor");
        call(get("/api/v1/products").param("q", "마그").param("size", "2").param("cursor", cursor))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.page.hasNext").value(false));
        call(get("/api/v1/products").param("q", "그%")).andExpect(jsonPath("$.data.length()").value(1));
        call(get("/api/v1/products").param("q", "마그").param("cursor", "!!")).andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidRequests() throws Exception {
        for (String body : List.of("{}", "not json",
                body("비타민D", "\"25:00\"", ALL_DAYS), body("비타민D", "\"08:30:15\"", ALL_DAYS),
                body("비타민D", "", ALL_DAYS), body(" ", "\"08:30\"", ALL_DAYS),
                body("비타민D", "\"08:30\"", "\"FOO\""),
                body("비타민D", "\"08:30\"", ALL_DAYS).replace("VIT_D", "NOPE"),
                body("비타민D", "\"08:30\"", ALL_DAYS).replace("μg", "IU"))) {
            call(post("/api/v1/supplements"), body).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
        }
        call(get("/api/v1/intakes?date=abc")).andExpect(status().isBadRequest());
        call(get("/api/v1/none")).andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        take(Long.MAX_VALUE).andExpect(status().isNotFound());
    }

    private static final String ALL_DAYS = "\"MON\",\"TUE\",\"WED\",\"THU\",\"FRI\",\"SAT\",\"SUN\"";

    private String body(String name, String times, String days) {
        return """
                {"newProduct":{"name":"%s","nutrients":[{"code":"VIT_D","amount":50,"unit":"μg"}]},
                 "dosePerIntake":2,"mealTiming":"AFTER","daysOfWeek":[%s],"times":[%s],"totalQty":90}
                """.formatted(name, days, times);
    }

    private ResultActions create(String name, String times, String days) throws Exception {
        return call(post("/api/v1/supplements"), body(name, times, days));
    }

    private long register(String name, String times, String days) throws Exception {
        String json = create(name, times, days).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.data.id")).longValue();
    }

    private ResultActions take(long id) throws Exception {
        return call(post("/api/v1/intakes/take"), "{\"intakeIds\":[" + id + "]}");
    }

    /** 요청마다 새 영속성 컨텍스트에서 시작하도록 비운다. */
    private ResultActions call(MockHttpServletRequestBuilder request) throws Exception {
        em.flush();
        em.clear();
        return mvc.perform(request);
    }

    private ResultActions call(MockHttpServletRequestBuilder request, String body) throws Exception {
        return call(request.contentType(MediaType.APPLICATION_JSON).characterEncoding("UTF-8").content(body));
    }

    private long firstLog(long supplementId) {
        return todayLogs(supplementId).get(0);
    }

    private List<Long> todayLogs(long supplementId) {
        Instant end = Instant.parse("2026-11-23T15:00:00Z");
        return logs.findByUserSupplementId(supplementId).stream().filter(l -> l.getScheduledAt().isBefore(end))
                .sorted((a, b) -> a.getScheduledAt().compareTo(b.getScheduledAt())).map(IntakeLog::getId).toList();
    }

    private DailySummary summary() {
        return summaries.findById(new DailySummaryId(userId, TODAY)).orElseThrow();
    }

    private Long newUser() {
        return users.save(new User(OAuthProvider.KAKAO, UUID.randomUUID().toString(), "테스트")).getId();
    }

    private void loginAs(Long id) {
        given(currentUser.currentUserId()).willReturn(Optional.of(id));
    }

    private void koreanTime(String local) {
        given(clock.instant()).willReturn(java.time.LocalDateTime.parse(local)
                .toInstant(java.time.ZoneOffset.ofHours(9)));
    }
}
