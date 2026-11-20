package com.renderia.renderia_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.renderia.renderia_backend.config.DemoAccountsInitializer;
import com.renderia.renderia_backend.dto.AuthResponse;
import com.renderia.renderia_backend.dto.DemoLoginRequest;
import com.renderia.renderia_backend.dto.LoginRequest;
import com.renderia.renderia_backend.dto.RegisterRequest;
import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.Role;
import com.renderia.renderia_backend.model.User;
import com.renderia.renderia_backend.repository.RoleRepository;
import com.renderia.renderia_backend.repository.UserRepository;
import com.renderia.renderia_backend.security.JwtService;
import com.renderia.renderia_backend.security.ratelimit.AuthRateLimiter;
import com.renderia.renderia_backend.support.TestUsers;

class AuthServiceTests {

    private static final long ADMIN_ROLE_ID = 1L;
    private static final long ENGINEER_ROLE_ID = 2L;

    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final AuthService service = new AuthService(users, roles, passwordEncoder, jwtService, new AuthRateLimiter());

    @BeforeEach
    void setUp() {
        when(roles.findById(ADMIN_ROLE_ID)).thenReturn(Optional.of(TestUsers.role(ADMIN_ROLE_ID, Role.ADMIN)));
        when(roles.findById(ENGINEER_ROLE_ID)).thenReturn(Optional.of(TestUsers.role(ENGINEER_ROLE_ID, Role.ENGINEER)));
        when(jwtService.generateToken(any())).thenReturn("token-de-prueba");
    }

    @Test
    void registersAnEngineer() {
        AuthResponse response = service.register(
                new RegisterRequest("  Ana Ruiz ", "ANA@Renderia.co ", "clave-segura-1", ENGINEER_ROLE_ID), "203.0.113.7");

        assertEquals("token-de-prueba", response.token());
        assertEquals("ana@renderia.co", response.user().email());
        assertEquals("Ana Ruiz", response.user().fullName());
    }

    @Test
    void cannotSelfRegisterAsAdministrator() {
        ApiException error = assertThrows(ApiException.class, () -> service.register(
                new RegisterRequest("Intruso", "intruso@renderia.co", "clave-segura-1", ADMIN_ROLE_ID), "203.0.113.7"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verify(users, never()).save(any());
    }

    @Test
    void rejectsShortPasswordsAndInvalidEmails() {
        assertThrows(ApiException.class, () -> service.register(
                new RegisterRequest("Ana", "ana@renderia.co", "corta", ENGINEER_ROLE_ID), "203.0.113.7"));
        assertThrows(ApiException.class, () -> service.register(
                new RegisterRequest("Ana", "esto-no-es-correo", "clave-segura-1", ENGINEER_ROLE_ID), "203.0.113.7"));
    }

    @Test
    void rejectsPasswordsLongerThanBcryptSupports() {
        String tooLong = "a".repeat(73);
        assertThrows(ApiException.class, () -> service.register(
                new RegisterRequest("Ana", "ana@renderia.co", tooLong, ENGINEER_ROLE_ID), "203.0.113.7"));
    }

    @Test
    void rejectsDuplicatedEmail() {
        when(users.existsByEmailIgnoreCase("ana@renderia.co")).thenReturn(true);
        ApiException error = assertThrows(ApiException.class, () -> service.register(
                new RegisterRequest("Ana", "ana@renderia.co", "clave-segura-1", ENGINEER_ROLE_ID), "203.0.113.7"));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
    }

    @Test
    void wrongPasswordGivesTheSameMessageAsUnknownEmail() {
        User user = TestUsers.user(5L, Role.ENGINEER, true);
        user.setPasswordHash(passwordEncoder.encode("clave-correcta"));
        when(users.findByEmailIgnoreCase("usuario5@renderia.co")).thenReturn(Optional.of(user));

        ApiException wrongPassword = assertThrows(ApiException.class,
                () -> service.login(new LoginRequest("usuario5@renderia.co", "otra-clave"), "203.0.113.7"));
        ApiException unknownEmail = assertThrows(ApiException.class,
                () -> service.login(new LoginRequest("nadie@renderia.co", "otra-clave"), "203.0.113.7"));

        assertEquals(HttpStatus.UNAUTHORIZED, wrongPassword.getStatus());
        assertEquals(wrongPassword.getMessage(), unknownEmail.getMessage());
    }

    @Test
    void deactivatedAccountCannotLogIn() {
        User user = TestUsers.user(6L, Role.ENGINEER, false);
        user.setPasswordHash(passwordEncoder.encode("clave-correcta"));
        when(users.findByEmailIgnoreCase("usuario6@renderia.co")).thenReturn(Optional.of(user));

        ApiException error = assertThrows(ApiException.class,
                () -> service.login(new LoginRequest("usuario6@renderia.co", "clave-correcta"), "203.0.113.7"));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
    }

    @Test
    void demoLoginOnlyWorksForEngineerAndStudent() {
        User engineer = TestUsers.user(7L, Role.ENGINEER, true);
        when(users.findByEmailIgnoreCase(DemoAccountsInitializer.ENGINEER_EMAIL)).thenReturn(Optional.of(engineer));

        assertEquals("token-de-prueba", service.demoLogin(new DemoLoginRequest(Role.ENGINEER)).token());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class,
                () -> service.demoLogin(new DemoLoginRequest(Role.ADMIN))).getStatus());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class,
                () -> service.demoLogin(new DemoLoginRequest(Role.STUDENT))).getStatus());
    }
}
