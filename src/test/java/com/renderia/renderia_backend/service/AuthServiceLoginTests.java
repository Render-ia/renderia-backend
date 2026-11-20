package com.renderia.renderia_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.renderia.renderia_backend.dto.LoginRequest;
import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.exception.TooManyRequestsException;
import com.renderia.renderia_backend.model.Role;
import com.renderia.renderia_backend.model.User;
import com.renderia.renderia_backend.repository.RoleRepository;
import com.renderia.renderia_backend.repository.UserRepository;
import com.renderia.renderia_backend.security.JwtService;
import com.renderia.renderia_backend.security.ratelimit.AuthRateLimiter;

class AuthServiceLoginTests {

    private UserRepository users;
    private PasswordEncoder encoder;
    private JwtService jwt;
    private AuthService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        encoder = mock(PasswordEncoder.class);
        jwt = mock(JwtService.class);
        when(encoder.encode(anyString())).thenReturn("$dummy");
        service = new AuthService(users, mock(RoleRepository.class), encoder, jwt, new AuthRateLimiter());
    }

    private static User user(String email, String hash) {
        Role role = mock(Role.class);
        when(role.getName()).thenReturn(Role.ENGINEER);
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(hash);
        u.setFullName("Ana");
        u.setRole(role);
        u.setActive(true);
        return u;
    }

    @Test
    void conCorreoInexistenteIgualSeComparaUnHashParaNoDelatarlo() {
        when(users.findByEmailIgnoreCase("nadie@x.co")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.login(new LoginRequest("nadie@x.co", "lo-que-sea"), "1.1.1.1"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(encoder).matches("lo-que-sea", "$dummy");
    }

    @Test
    void despuesDeCincoFallosResponde429SinRevisarLaContrasena() {
        User ana = user("ana@x.co", "$hash");
        when(users.findByEmailIgnoreCase("ana@x.co")).thenReturn(Optional.of(ana));
        when(encoder.matches(anyString(), eq("$hash"))).thenReturn(false);
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.login(new LoginRequest("ana@x.co", "mala"), "1.1.1.1"))
                    .isNotInstanceOf(TooManyRequestsException.class);
        }
        when(encoder.matches("buena", "$hash")).thenReturn(true);
        assertThatThrownBy(() -> service.login(new LoginRequest("ana@x.co", "buena"), "1.1.1.1"))
                .isInstanceOf(TooManyRequestsException.class);
        verify(encoder, never()).matches("buena", "$hash");
    }

    @Test
    void elCorreoSeNormalizaAntesDeContarIntentos() {
        when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        for (int i = 0; i < 5; i++) {
            String variant = i % 2 == 0 ? " ANA@x.co " : "ana@X.CO";
            String ip = "1.1.1." + i;
            assertThatThrownBy(() -> service.login(new LoginRequest(variant, "mala"), ip))
                    .isNotInstanceOf(TooManyRequestsException.class);
        }
        assertThatThrownBy(() -> service.login(new LoginRequest("ana@x.co", "mala"), "8.8.8.8"))
                .isInstanceOf(TooManyRequestsException.class);
    }
}
