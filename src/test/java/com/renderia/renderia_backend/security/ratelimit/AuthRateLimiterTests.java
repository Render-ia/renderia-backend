package com.renderia.renderia_backend.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.renderia.renderia_backend.exception.TooManyRequestsException;

class AuthRateLimiterTests {

    /** Reloj que la prueba puede adelantar. */
    static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-20T10:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void blocksTheEmailAfterFiveFailures() {
        MutableClock clock = new MutableClock();
        AuthRateLimiter limiter = new AuthRateLimiter(clock);
        for (int i = 0; i < 5; i++) {
            limiter.checkLogin("ana@x.co", "1.1.1.1");
            limiter.loginFailed("ana@x.co", "1.1.1.1");
        }
        assertThatThrownBy(() -> limiter.checkLogin("ana@x.co", "2.2.2.2"))
                .isInstanceOf(TooManyRequestsException.class)
                .satisfies(e -> assertThat(((TooManyRequestsException) e).getRetryAfterSeconds()).isBetween(1L, 900L));
        // Otro correo desde otra IP no se ve afectado.
        assertThatCode(() -> limiter.checkLogin("luis@x.co", "3.3.3.3")).doesNotThrowAnyException();
    }

    @Test
    void blockIsLiftedWhenTheWindowPasses() {
        MutableClock clock = new MutableClock();
        AuthRateLimiter limiter = new AuthRateLimiter(clock);
        for (int i = 0; i < 5; i++) {
            limiter.loginFailed("ana@x.co", "1.1.1.1");
        }
        clock.advance(Duration.ofMinutes(15).plusSeconds(1));
        assertThatCode(() -> limiter.checkLogin("ana@x.co", "1.1.1.1")).doesNotThrowAnyException();
    }

    @Test
    void successfulLoginClearsTheEmailCounter() {
        AuthRateLimiter limiter = new AuthRateLimiter(new MutableClock());
        for (int i = 0; i < 4; i++) {
            limiter.loginFailed("ana@x.co", "1.1.1.1");
        }
        limiter.loginSucceeded("ana@x.co");
        limiter.loginFailed("ana@x.co", "1.1.1.1");
        assertThatCode(() -> limiter.checkLogin("ana@x.co", "1.1.1.1")).doesNotThrowAnyException();
    }

    @Test
    void blocksAnIpThatTriesManyEmails() {
        AuthRateLimiter limiter = new AuthRateLimiter(new MutableClock());
        for (int i = 0; i < 30; i++) {
            limiter.loginFailed("usuario" + i + "@x.co", "9.9.9.9");
        }
        assertThatThrownBy(() -> limiter.checkLogin("nuevo@x.co", "9.9.9.9"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void limitsMassRegistrationPerIp() {
        AuthRateLimiter limiter = new AuthRateLimiter(new MutableClock());
        for (int i = 0; i < 10; i++) {
            limiter.checkRegister("4.4.4.4");
            limiter.registered("4.4.4.4");
        }
        assertThatThrownBy(() -> limiter.checkRegister("4.4.4.4")).isInstanceOf(TooManyRequestsException.class);
    }
}
