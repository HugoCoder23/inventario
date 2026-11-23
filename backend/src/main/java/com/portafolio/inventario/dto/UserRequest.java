package com.portafolio.inventario.dto;

import com.portafolio.inventario.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta o edición de usuario. Al crear, la contraseña es obligatoria;
 * al editar, si se deja vacía se conserva la actual.
 */
public record UserRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String name,
        @NotBlank(message = "El email es obligatorio") @Email(message = "El email no es válido") @Size(max = 160) String email,
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String password,
        @NotNull(message = "El rol es obligatorio") Role role,
        Boolean active) {
}
