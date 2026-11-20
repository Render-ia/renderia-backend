package com.renderia.renderia_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.renderia.renderia_backend.dto.AuthResponse;
import com.renderia.renderia_backend.dto.DemoLoginRequest;
import com.renderia.renderia_backend.dto.LoginRequest;
import com.renderia.renderia_backend.dto.RegisterRequest;
import com.renderia.renderia_backend.dto.UserResponse;
import com.renderia.renderia_backend.security.ratelimit.ClientIpResolver;
import com.renderia.renderia_backend.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final ClientIpResolver clientIp;

    public AuthController(AuthService authService, ClientIpResolver clientIp) {
        this.authService = authService;
        this.clientIp = clientIp;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request, clientIp.resolve(http)));
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request, clientIp.resolve(http));
    }

    @PostMapping("/demo")
    public AuthResponse demoLogin(@RequestBody DemoLoginRequest request) {
        return authService.demoLogin(request);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.me(Long.valueOf(jwt.getSubject()));
    }
}
