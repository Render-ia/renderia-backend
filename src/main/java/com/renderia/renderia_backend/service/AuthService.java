package com.renderia.renderia_backend.service;

import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renderia.renderia_backend.dto.AuthResponse;
import com.renderia.renderia_backend.dto.LoginRequest;
import com.renderia.renderia_backend.dto.RegisterRequest;
import com.renderia.renderia_backend.dto.UserResponse;
import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.Role;
import com.renderia.renderia_backend.model.User;
import com.renderia.renderia_backend.repository.RoleRepository;
import com.renderia.renderia_backend.repository.UserRepository;
import com.renderia.renderia_backend.security.JwtService;

/** Registro, inicio de sesión y consulta del usuario actual. */
@Service
public class AuthService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Set<String> SELF_REGISTER_ROLES = Set.of(Role.ENGINEER, Role.STUDENT);
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, RoleRepository roles,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request == null || isBlank(request.fullName())) {
            throw ApiException.badRequest("El nombre completo es obligatorio.");
        }
        String email = normalizeEmail(request.email());
        if (!EMAIL.matcher(email).matches()) {
            throw ApiException.badRequest("El correo no es válido.");
        }
        if (request.password() == null || request.password().length() < MIN_PASSWORD_LENGTH) {
            throw ApiException.badRequest("La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres.");
        }
        Role role = roles.findById(request.roleId() == null ? -1L : request.roleId())
                .filter(r -> SELF_REGISTER_ROLES.contains(r.getName()))
                .orElseThrow(() -> ApiException.badRequest("Solo puedes registrarte como Ingeniero o Estudiante."));
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Ya existe una cuenta con ese correo.");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        users.save(user);

        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request == null ? null : request.email());
        String password = request == null ? null : request.password();

        User user = users.findByEmailIgnoreCase(email)
                .filter(u -> password != null && passwordEncoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos."));
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tu cuenta está desactivada. Contacta al administrador.");
        }
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
