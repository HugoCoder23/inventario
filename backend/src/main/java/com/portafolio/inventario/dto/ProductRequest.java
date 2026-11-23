package com.portafolio.inventario.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Alta o edición de producto. {@code initialStock} solo se usa al crear: genera un
 * movimiento de entrada "Stock inicial" en la misma transacción. El stock nunca se
 * edita directamente; cambia únicamente mediante movimientos.
 */
public record ProductRequest(
        @NotBlank(message = "El SKU es obligatorio")
        @Size(max = 40, message = "El SKU admite máximo 40 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*$", message = "El SKU solo admite letras, números, punto, guion y guion bajo")
        String sku,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre admite máximo 150 caracteres")
        String name,

        @Size(max = 1000, message = "La descripción admite máximo 1000 caracteres")
        String description,

        @NotNull(message = "La categoría es obligatoria")
        Long categoryId,

        @NotNull(message = "El costo es obligatorio")
        @DecimalMin(value = "0.00", message = "El costo no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El costo admite hasta 10 enteros y 2 decimales")
        BigDecimal cost,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio admite hasta 10 enteros y 2 decimales")
        BigDecimal price,

        @NotNull(message = "El stock mínimo es obligatorio")
        @Min(value = 0, message = "El stock mínimo no puede ser negativo")
        @Max(value = 1_000_000, message = "El stock mínimo es demasiado grande")
        Integer minStock,

        @Min(value = 0, message = "El stock inicial no puede ser negativo")
        @Max(value = 1_000_000, message = "El stock inicial es demasiado grande")
        Integer initialStock) {
}
