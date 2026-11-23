package com.portafolio.inventario.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        BigDecimal cost,
        BigDecimal price,
        int minStock,
        int stock,
        boolean lowStock,
        boolean active,
        BigDecimal stockValue,
        Instant createdAt,
        Instant updatedAt) {
}
