package com.renderia.renderia_backend.security.quota;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.renderia.renderia_backend.exception.ApiException;

class DailyQuotaTests {

    private static final Instant MORNING = Instant.parse("2026-11-20T08:00:00Z");

    @Test
    void allowsUpToTheLimitAndAnswers429After() {
        DailyQuota quota = new DailyQuota(Clock.fixed(MORNING, ZoneOffset.UTC));
        quota.take("copilot:1", 2, "límite");
        quota.take("copilot:1", 2, "límite");

        assertThatThrownBy(() -> quota.take("copilot:1", 2, "límite"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        assertThat(quota.remaining("copilot:1", 2)).isZero();
    }

    @Test
    void keepsUsersApart() {
        DailyQuota quota = new DailyQuota(Clock.fixed(MORNING, ZoneOffset.UTC));
        quota.take("copilot:1", 1, "límite");

        quota.take("copilot:2", 1, "límite");
        assertThat(quota.remaining("copilot:2", 1)).isZero();
    }

    @Test
    void startsAgainTheNextUtcDay() {
        MutableClock clock = new MutableClock(MORNING);
        DailyQuota quota = new DailyQuota(clock);
        quota.take("render:1", 1, "límite");

        clock.advance(Duration.ofDays(1));

        quota.take("render:1", 1, "límite");
        assertThat(quota.remaining("render:1", 1)).isZero();
    }

    @Test
    void aRefusedCallDoesNotCount() {
        DailyQuota quota = new DailyQuota(Clock.fixed(MORNING, ZoneOffset.UTC));
        quota.take("copilot:1", 1, "límite");
        assertThatThrownBy(() -> quota.take("copilot:1", 1, "límite")).isInstanceOf(ApiException.class);

        assertThat(quota.remaining("copilot:1", 3)).isEqualTo(2);
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void aFailedCallGivesItsUnitBack() {
        DailyQuota quota = new DailyQuota(Clock.fixed(MORNING, ZoneOffset.UTC));
        quota.take("copilot:1", 2, "límite");
        quota.giveBack("copilot:1");
        assertThat(quota.remaining("copilot:1", 2)).isEqualTo(2);
    }

    @Test
    void givingBackNeverGoesBelowZero() {
        DailyQuota quota = new DailyQuota(Clock.fixed(MORNING, ZoneOffset.UTC));
        quota.giveBack("copilot:1");
        quota.take("copilot:1", 1, "límite");
        quota.giveBack("copilot:1");
        quota.giveBack("copilot:1");
        assertThat(quota.remaining("copilot:1", 1)).isEqualTo(1);
    }
}
