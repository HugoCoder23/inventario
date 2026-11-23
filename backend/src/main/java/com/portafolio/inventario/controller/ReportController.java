package com.portafolio.inventario.controller;

import com.portafolio.inventario.dto.DashboardResponse;
import com.portafolio.inventario.dto.MovementSummaryResponse;
import com.portafolio.inventario.dto.ProductResponse;
import com.portafolio.inventario.entity.MovementType;
import com.portafolio.inventario.service.ReportService;
import com.portafolio.inventario.service.ReportService.DateRange;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Reportes y dashboard")
public class ReportController {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);
    private static final String BOM = "﻿"; // Para que Excel detecte UTF-8 (acentos).

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Indicadores del dashboard")
    public DashboardResponse dashboard() {
        return reportService.dashboard();
    }

    @GetMapping("/reports/low-stock")
    @Operation(summary = "Productos activos con stock igual o menor al mínimo")
    public List<ProductResponse> lowStock() {
        return reportService.lowStock();
    }

    @GetMapping("/reports/movements")
    @Operation(summary = "Resumen de movimientos por periodo, producto y tipo",
            description = "Por defecto, los últimos 30 días. Fechas yyyy-MM-dd inclusivas en la zona 'tz'.")
    public MovementSummaryResponse movementSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String tz,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) MovementType type) {
        return reportService.movementSummary(DateRange.of(from, to, tz), productId, type);
    }

    @GetMapping(value = "/reports/products/export", produces = "text/csv")
    @Operation(summary = "Exportar productos a CSV (acepta los mismos filtros que el listado)")
    public ResponseEntity<String> exportProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean lowStock) {
        return csv("productos.csv", reportService.productsCsv(q, categoryId, active, lowStock));
    }

    @GetMapping(value = "/reports/movements/export", produces = "text/csv")
    @Operation(summary = "Exportar movimientos a CSV (acepta los mismos filtros que el historial)")
    public ResponseEntity<String> exportMovements(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String tz,
            @RequestParam(required = false) String q) {
        DateRange range = DateRange.optional(from, to, tz);
        return csv("movimientos.csv", reportService.movementsCsv(productId, type, range, q));
    }

    private ResponseEntity<String> csv(String filename, String body) {
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(BOM + body);
    }
}
