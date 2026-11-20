package com.renderia.renderia_backend.security;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import com.renderia.renderia_backend.model.User;
import com.renderia.renderia_backend.repository.UserRepository;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Reglas de acceso de la API:
 * - Públicas: hello, health, login, registro, acceso a las cuentas de prueba y la lectura de catálogos.
 * - Imágenes de planos: sin token, pero solo con el enlace firmado que entrega la API.
 * - Solo administrador: crear, editar o borrar catálogos y modelos de IA, y gestionar usuarios.
 * - Todo lo demás exige un token válido (Authorization: Bearer ...).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] CATALOGS = {
        "/api/v1/building-types/**", "/api/v1/element-types/**", "/api/v1/materials/**", "/api/v1/ai-models/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserRepository users) throws Exception {
        http
            // Sin cookies de sesión (el token va en Authorization), así que CSRF no aplica.
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000)))
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/v1/hello", "/api/v1/health").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/demo").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/roles").permitAll()
                .requestMatchers(HttpMethod.GET, CATALOGS).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/floor-plans/*/image").permitAll()
                .requestMatchers(CATALOGS).hasRole("ADMINISTRADOR")
                .requestMatchers("/api/v1/users/**").hasRole("ADMINISTRADOR")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.jwtAuthenticationConverter(activeSessionConverter(users)))
                .authenticationEntryPoint(unauthorized()))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint(unauthorized())
                .accessDeniedHandler(forbidden()));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Antes de aceptar un token revisa en la base de datos que el usuario siga existiendo,
     * esté activo y conserve el mismo rol. Así, si el administrador desactiva una cuenta
     * o le cambia el rol, sus sesiones abiertas dejan de servir de inmediato.
     */
    private Converter<Jwt, AbstractAuthenticationToken> activeSessionConverter(UserRepository users) {
        JwtAuthenticationConverter roles = jwtAuthenticationConverter();
        return jwt -> {
            Long userId;
            try {
                userId = Long.valueOf(jwt.getSubject());
            } catch (NumberFormatException e) {
                throw new BadCredentialsException("Token sin usuario válido.");
            }
            String tokenRole = jwt.getClaimAsString("role");
            boolean valid = users.findById(userId)
                    .filter(User::isActive)
                    .filter(user -> user.getRole().getName().equals(tokenRole))
                    .isPresent();
            if (!valid) {
                throw new BadCredentialsException("La cuenta ya no está activa o cambió de rol.");
            }
            return roles.convert(jwt);
        };
    }

    /** Convierte el rol del token ("Administrador") en la autoridad ROLE_ADMINISTRADOR. */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        Converter<Jwt, Collection<GrantedAuthority>> roles = jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null) {
                return List.of();
            }
            return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)));
        };
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    private AuthenticationEntryPoint unauthorized() {
        return (request, response, e) -> writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                "Tu sesión no es válida o expiró. Inicia sesión de nuevo.");
    }

    private AccessDeniedHandler forbidden() {
        return (request, response, e) -> writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                "No tienes permiso para hacer esto.");
    }

    private static void writeJson(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
