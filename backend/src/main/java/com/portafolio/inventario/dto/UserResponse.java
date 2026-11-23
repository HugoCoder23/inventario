package com.portafolio.inventario.dto;

import com.portafolio.inventario.entity.Role;

import java.time.Instant;

public record UserResponse(Long id, String name, String email, Role role, boolean active, Instant createdAt) {
}
