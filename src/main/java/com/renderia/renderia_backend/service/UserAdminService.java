package com.renderia.renderia_backend.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renderia.renderia_backend.dto.UserResponse;
import com.renderia.renderia_backend.dto.UserUpdateRequest;
import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.Role;
import com.renderia.renderia_backend.model.User;
import com.renderia.renderia_backend.repository.RoleRepository;
import com.renderia.renderia_backend.repository.UserRepository;

/** Gestión de usuarios para el administrador. Siempre debe quedar un administrador activo. */
@Service
public class UserAdminService {

    private final UserRepository users;
    private final RoleRepository roles;

    public UserAdminService(UserRepository users, RoleRepository roles) {
        this.users = users;
        this.roles = roles;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return users.findAll(Sort.by("id")).stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = users.findById(id).orElseThrow(() -> ApiException.notFound("El usuario no existe."));
        boolean wasActiveAdmin = isActiveAdmin(user);

        if (request.roleId() != null) {
            Role role = roles.findById(request.roleId())
                    .orElseThrow(() -> ApiException.badRequest("El rol no existe."));
            user.setRole(role);
        }
        if (request.isActive() != null) {
            user.setActive(request.isActive());
        }

        if (wasActiveAdmin && !isActiveAdmin(user) && countActiveAdmins() <= 1) {
            throw ApiException.badRequest("Debe quedar al menos un administrador activo.");
        }
        return UserResponse.from(users.save(user));
    }

    private long countActiveAdmins() {
        return users.findAll().stream().filter(UserAdminService::isActiveAdmin).count();
    }

    private static boolean isActiveAdmin(User user) {
        return user.isActive() && Role.ADMIN.equals(user.getRole().getName());
    }
}
