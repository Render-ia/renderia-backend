package com.renderia.renderia_backend.controller;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.renderia.renderia_backend.dto.RoleResponse;
import com.renderia.renderia_backend.repository.RoleRepository;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleRepository roles;

    public RoleController(RoleRepository roles) {
        this.roles = roles;
    }

    @GetMapping
    public List<RoleResponse> list() {
        return roles.findAll(Sort.by("id")).stream().map(RoleResponse::from).toList();
    }
}
