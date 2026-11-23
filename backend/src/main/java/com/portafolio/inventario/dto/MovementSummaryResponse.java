package com.portafolio.inventario.dto;

import com.portafolio.inventario.entity.MovementType;

import java.time.LocalDate;
import java.util.List;

/** Reporte de movimientos de un periodo, agregado por tipo, por producto y por día. */
public record MovementSummaryResponse(
        LocalDate from,
        LocalDate to,
        long totalMovements,
        long unitsIn,
        long unitsOut,
        long netChange,
        List<TypeTotal> byType,
        List<ProductTotal> byProduct,
        List<DayTotal> byDay) {

    public record TypeTotal(MovementType type, long movements, long units, long netChange) {}

    public record ProductTotal(Long productId, String sku, String name, long movements,
                               long unitsIn, long unitsOut, long netChange) {}

    public record DayTotal(LocalDate date, long unitsIn, long unitsOut) {}
}
