package com.example.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.app.character.GameCharacter;
import com.example.app.character.GameCharacterRepository;
import com.example.app.intake.IntakeLog;
import com.example.app.intake.IntakeLogRepository;
import com.example.app.notification.NotificationSetting;
import com.example.app.notification.NotificationSettingRepository;
import com.example.app.product.Product;
import com.example.app.product.ProductRepository;
import com.example.app.product.ProductSource;
import com.example.app.reward.RewardLedger;
import com.example.app.reward.RewardLedgerRepository;
import com.example.app.reward.RewardSourceType;
import com.example.app.supplement.SupplementSchedule;
import com.example.app.supplement.SupplementScheduleRepository;
import com.example.app.supplement.UserSupplement;
import com.example.app.supplement.UserSupplementRepository;
import com.example.app.user.OAuthProvider;
import com.example.app.user.User;
import com.example.app.user.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SchemaTests {

    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired UserSupplementRepository supplements;
    @Autowired SupplementScheduleRepository schedules;
    @Autowired IntakeLogRepository intakes;
    @Autowired GameCharacterRepository characters;
    @Autowired RewardLedgerRepository rewards;
    @Autowired NotificationSettingRepository notificationSettings;

    Long userId;

    @BeforeEach
    void setUp() {
        userId = users.save(new User(OAuthProvider.KAKAO, UUID.randomUUID().toString(), "테스트")).getId();
    }

    @Test
    void everyMappedTableIsReadable() {
        em.getMetamodel().getEntities().forEach(entity ->
                em.createQuery("select e from " + entity.getName() + " e").setMaxResults(1).getResultList());
        assertThat(em.getMetamodel().getEntities()).hasSize(18);
    }

    @Test
    void storesScheduleInLocalTimeAndLogInUtc() {
        SupplementSchedule schedule = saveSchedule();
        Instant scheduledAt = Instant.parse("2026-11-22T23:30:00Z");
        IntakeLog log = intakes.saveAndFlush(new IntakeLog(userId, schedule.getUserSupplementId(), schedule.getId(), scheduledAt));

        assertThat(jdbc.queryForObject("select cast(intake_time as char(8)) from supplement_schedules where id = ?",
                String.class, schedule.getId())).startsWith("08:30:00");
        assertThat(jdbc.queryForObject("select cast(scheduled_at as char(19)) from intake_logs where id = ?",
                String.class, log.getId())).startsWith("2026-11-22 23:30:00");

        em.clear();
        assertThat(schedules.findById(schedule.getId()).orElseThrow().getIntakeTime()).isEqualTo(LocalTime.of(8, 30));
        assertThat(intakes.findById(log.getId()).orElseThrow().getScheduledAt()).isEqualTo(scheduledAt);
    }

    @Test
    void databaseDefaultTimeIsUtc() {
        jdbc.update("insert into users (provider, provider_user_id) values ('GOOGLE', ?)", "default-time-" + userId);
        Instant createdAt = jdbc.queryForObject("select created_at from users where provider_user_id = ?",
                java.time.LocalDateTime.class, "default-time-" + userId).toInstant(java.time.ZoneOffset.UTC);
        assertThat(createdAt).isBetween(Instant.now().minusSeconds(60), Instant.now().plusSeconds(60));
    }

    @Test
    void rejectsDuplicateIntakeSlot() {
        SupplementSchedule schedule = saveSchedule();
        Instant at = Instant.parse("2026-11-22T23:30:00Z");
        intakes.saveAndFlush(new IntakeLog(userId, schedule.getUserSupplementId(), schedule.getId(), at));
        assertThatThrownBy(() -> intakes.saveAndFlush(
                new IntakeLog(userId, schedule.getUserSupplementId(), schedule.getId(), at)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsRewardAgainAfterCancelButNotTwice() {
        Long characterId = characters.save(new GameCharacter(userId, "CAT", "냥이")).getId();
        RewardLedger first = rewards.saveAndFlush(reward(characterId, RewardSourceType.INTAKE, 1L, 0, 10));
        rewards.saveAndFlush(reward(characterId, RewardSourceType.CANCEL, first.getId(), 0, -10));
        RewardLedger second = rewards.saveAndFlush(reward(characterId, RewardSourceType.INTAKE, 1L, 1, 10));
        rewards.saveAndFlush(reward(characterId, RewardSourceType.CANCEL, second.getId(), 0, -10));

        assertThatThrownBy(() -> rewards.saveAndFlush(reward(characterId, RewardSourceType.INTAKE, 1L, 1, 10)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsOnlyOneActiveCharacter() {
        GameCharacter first = characters.saveAndFlush(new GameCharacter(userId, "CAT", "첫째"));
        first.complete(Instant.now());
        characters.saveAndFlush(first);
        GameCharacter second = characters.saveAndFlush(new GameCharacter(userId, "DOG", "둘째"));
        second.complete(Instant.now());
        characters.saveAndFlush(second);
        characters.saveAndFlush(new GameCharacter(userId, "CAT", "셋째"));

        assertThatThrownBy(() -> characters.saveAndFlush(new GameCharacter(userId, "CAT", "넷째")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingUserCascadesToSettings() {
        notificationSettings.saveAndFlush(new NotificationSetting(userId));
        jdbc.update("delete from users where id = ?", userId);
        em.clear();
        assertThat(notificationSettings.findById(userId)).isEmpty();
    }

    private SupplementSchedule saveSchedule() {
        Long productId = products.save(new Product("비타민D", null, ProductSource.USER, userId)).getId();
        Long supplementId = supplements.save(new UserSupplement(userId, productId, LocalDate.of(2026, 11, 23))).getId();
        return schedules.saveAndFlush(new SupplementSchedule(supplementId, LocalTime.of(8, 30)));
    }

    private RewardLedger reward(Long characterId, RewardSourceType type, Long sourceId, int seq, int exp) {
        return new RewardLedger(userId, characterId, type, sourceId, seq, exp);
    }
}
