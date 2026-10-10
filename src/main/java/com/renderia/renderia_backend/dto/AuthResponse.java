package com.renderia.renderia_backend.dto;

/** Respuesta de login y registro: el token de sesión y el usuario. */
public record AuthResponse(String token, UserResponse user) {
}
