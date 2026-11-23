package com.portafolio.inventario.dto;

import com.portafolio.inventario.entity.MovementType;
import com.portafolio.inventario.validation.ValidMovement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@ValidMovement
public record StockMovementRequest(
        @NotNull(message = "El producto es obligatorio")
        Long productId,

        @NotNull(message = "El tipo de movimiento es obligatorio")
        MovementType type,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor que cero")
        @Max(value = 1_000_000, message = "La cantidad es demasiado grande")
        Integer quantity,

        /* Obligatorio solo para ajustes. */
        AdjustmentDirection direction,

        @Size(max = 255, message = "El motivo admite máximo 255 caracteres")
        String reason,

        @Size(max = 100, message = "La referencia admite máximo 100 caracteres")
        String reference) {
}
