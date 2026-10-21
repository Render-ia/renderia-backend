package com.renderia.renderia_backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.renderia.renderia_backend.dto.UserResponse;
import com.renderia.renderia_backend.dto.UserUpdateRequest;
import com.renderia.renderia_backend.service.UserAdminService;

/** Solo administrador (ver SecurityConfig). */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserAdminService service;

    public UserController(UserAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserResponse> list() {
        return service.list();
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return service.update(id, request);
    }
}
