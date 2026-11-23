package com.portafolio.inventario.dto;

import com.portafolio.inventario.entity.MovementType;

import java.time.Instant;

public record StockMovementResponse(
        Long id,
        Long productId,
        String productSku,
        String productName,
        MovementType type,
        int quantity,
        int delta,
        int stockBefore,
        int stockAfter,
        String reason,
        String reference,
        Long userId,
        String userName,
        Instant createdAt) {
}
