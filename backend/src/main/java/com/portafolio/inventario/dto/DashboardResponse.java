package com.portafolio.inventario.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        long totalProducts,
        long totalUnits,
        BigDecimal inventoryCostValue,
        BigDecimal inventoryRetailValue,
        long lowStockCount,
        long categoryCount,
        List<ProductResponse> lowStockProducts,
        List<StockMovementResponse> recentMovements) {
}
