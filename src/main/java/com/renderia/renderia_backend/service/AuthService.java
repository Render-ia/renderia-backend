package com.renderia.renderia_backend.service;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renderia.renderia_backend.validation.InputValidator;
import com.renderia.renderia_backend.dto.AuthResponse;
import com.renderia.renderia_backend.dto.DemoLoginRequest;
import com.renderia.renderia_backend.dto.LoginRequest;
import com.renderia.renderia_backend.dto.RegisterRequest;
import com.renderia.renderia_backend.dto.UserResponse;
import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.Role;
import com.renderia.renderia_backend.model.User;
import com.renderia.renderia_backend.repository.RoleRepository;
import com.renderia.renderia_backend.repository.UserRepository;
import com.renderia.renderia_backend.security.DemoAccounts;
import com.renderia.renderia_backend.security.JwtService;
import com.renderia.renderia_backend.security.ratelimit.AuthRateLimiter;

/** Registro, inicio de sesión y consulta del usuario actual. */
@Service
public class AuthService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Set<String> SELF_REGISTER_ROLES = Set.of(Role.ENGINEER, Role.STUDENT, Role.CLIENT);
    private static final int MIN_PASSWORD_LENGTH = 8;
    /** BCrypt solo tiene en cuenta los primeros 72 bytes de la contraseña. */
    private static final int MAX_PASSWORD_LENGTH = 72;

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthRateLimiter rateLimiter;
    /**
     * Hash de una contraseña aleatoria. Cuando el correo no existe se compara contra
     * este hash para que la respuesta tarde lo mismo y no delate qué correos están registrados.
     */
    private final String dummyHash;

    public AuthService(UserRepository users, RoleRepository roles,
                       PasswordEncoder passwordEncoder, JwtService jwtService, AuthRateLimiter rateLimiter) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rateLimiter = rateLimiter;
        this.dummyHash = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String clientIp) {
        rateLimiter.checkRegister(clientIp);
        if (request == null) {
            throw ApiException.badRequest("Envía los datos del registro.");
        }
        String fullName = InputValidator.requiredText(request.fullName(), "El nombre completo", 120);
        String email = InputValidator.requiredText(normalizeEmail(request.email()), "El correo", 150);
        if (!EMAIL.matcher(email).matches()) {
            throw ApiException.badRequest("El correo no es válido.");
        }
        if (request.password() == null || request.password().length() < MIN_PASSWORD_LENGTH) {
            throw ApiException.badRequest("La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres.");
        }
        if (request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_PASSWORD_LENGTH) {
            throw ApiException.badRequest("La contraseña admite máximo " + MAX_PASSWORD_LENGTH + " caracteres.");
        }
        Role role = roles.findById(request.roleId() == null ? -1L : request.roleId())
                .filter(r -> SELF_REGISTER_ROLES.contains(r.getName()))
                .orElseThrow(() -> ApiException.badRequest("Solo puedes registrarte como Ingeniero o Estudiante."));
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Ya existe una cuenta con ese correo.");
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        users.save(user);
        rateLimiter.registered(clientIp);

        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request, String clientIp) {
        String email = normalizeEmail(request == null ? null : request.email());
        String password = request == null ? null : request.password();
        rateLimiter.checkLogin(email, clientIp);

        Optional<User> found = users.findByEmailIgnoreCase(email);
        String hash = found.map(User::getPasswordHash).orElse(dummyHash);
        boolean matches = password != null && passwordEncoder.matches(password, hash);
        if (found.isEmpty() || !matches) {
            rateLimiter.loginFailed(email, clientIp);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos.");
        }
        rateLimiter.loginSucceeded(email);
        User user = found.get();
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tu cuenta está desactivada. Contacta al administrador.");
        }
        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }

    /**
     * Direct entry to the demo accounts (login buttons), so the frontend never
     * needs their passwords. Works for any of the five roles while the account is active.
     */
    @Transactional(readOnly = true)
    public AuthResponse demoLogin(DemoLoginRequest request) {
        String role = request == null || request.role() == null ? "" : request.role().trim();
        DemoAccounts.Account account = DemoAccounts.forRole(role)
                .orElseThrow(() -> ApiException.badRequest("No hay cuenta de prueba para ese rol."));
        User user = users.findByEmailIgnoreCase(account.email())
                .filter(User::isActive)
                .orElseThrow(() -> ApiException.notFound("La cuenta de prueba de " + role + " no está habilitada."));
        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Tu sesión no es válida. Inicia sesión de nuevo."));
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tu cuenta está desactivada. Contacta al administrador.");
        }
        return UserResponse.from(user);
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

}
