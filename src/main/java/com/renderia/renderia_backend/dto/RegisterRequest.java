package com.renderia.renderia_backend.dto;

public record RegisterRequest(String fullName, String email, String password, Long roleId) {
}
