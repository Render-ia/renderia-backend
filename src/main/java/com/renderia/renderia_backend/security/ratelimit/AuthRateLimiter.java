package com.renderia.renderia_backend.security.ratelimit;

import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.renderia.renderia_backend.exception.TooManyRequestsException;

/**
 * Frena la fuerza bruta en el inicio de sesión y el registro masivo de cuentas.
 *
 * - Login: 5 intentos fallidos por correo cada 15 minutos y 30 por IP. Un acierto
 *   limpia el contador del correo.
 * - Registro: 10 cuentas nuevas por IP cada hora.
 */
@Component
public class AuthRateLimiter {

    private final AttemptLimiter loginByEmail;
    private final AttemptLimiter loginByIp;
    private final AttemptLimiter registerByIp;

    @Autowired
    public AuthRateLimiter() {
        this(Clock.systemUTC());
    }

    public AuthRateLimiter(Clock clock) {
        this.loginByEmail = new AttemptLimiter(5, Duration.ofMinutes(15), clock);
        this.loginByIp = new AttemptLimiter(30, Duration.ofMinutes(15), clock);
        this.registerByIp = new AttemptLimiter(10, Duration.ofHours(1), clock);
    }

    public void checkLogin(String email, String ip) {
        long wait = Math.max(loginByEmail.secondsUntilAllowed(email), loginByIp.secondsUntilAllowed(ip));
        if (wait > 0) {
            throw new TooManyRequestsException(
                    "Demasiados intentos fallidos. Espera " + minutes(wait) + " e inténtalo de nuevo.", wait);
        }
    }

    public void loginFailed(String email, String ip) {
        loginByEmail.record(email);
        loginByIp.record(ip);
    }

    public void loginSucceeded(String email) {
        loginByEmail.reset(email);
    }

    public void checkRegister(String ip) {
        long wait = registerByIp.secondsUntilAllowed(ip);
        if (wait > 0) {
            throw new TooManyRequestsException(
                    "Se crearon demasiadas cuentas desde esta conexión. Espera " + minutes(wait) + ".", wait);
        }
    }

    public void registered(String ip) {
        registerByIp.record(ip);
    }

    private static String minutes(long seconds) {
        long m = Math.max(1, (seconds + 59) / 60);
        return m == 1 ? "1 minuto" : m + " minutos";
    }
}
