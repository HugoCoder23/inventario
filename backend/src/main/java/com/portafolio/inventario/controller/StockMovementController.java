package com.portafolio.inventario.controller;

import com.portafolio.inventario.dto.PageResponse;
import com.portafolio.inventario.dto.StockMovementRequest;
import com.portafolio.inventario.dto.StockMovementResponse;
import com.portafolio.inventario.entity.MovementType;
import com.portafolio.inventario.service.ReportService.DateRange;
import com.portafolio.inventario.service.StockMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stock-movements")
@Tag(name = "Movimientos de inventario")
public class StockMovementController {

    static final Map<String, String> SORTABLE = new LinkedHashMap<>();
    static {
        SORTABLE.put("createdAt", "createdAt");
        SORTABLE.put("quantity", "quantity");
        SORTABLE.put("type", "type");
        SORTABLE.put("product", "product.name");
    }

    private final StockMovementService movementService;

    public StockMovementController(StockMovementService movementService) {
        this.movementService = movementService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    @Operation(summary = "Registrar entrada, salida o ajuste",
            description = "Actualiza el stock y guarda el movimiento en una sola transacción. "
                    + "Una salida no puede superar el stock disponible; un ajuste requiere 'direction' y 'reason'.")
    public ResponseEntity<StockMovementResponse> register(@Valid @RequestBody StockMovementRequest request) {
        StockMovementResponse created = movementService.register(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @GetMapping
    @Operation(summary = "Consultar historial de movimientos",
            description = "Fechas en formato yyyy-MM-dd (inclusivas) interpretadas en la zona 'tz' (por defecto UTC).")
    public PageResponse<StockMovementResponse> list(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String tz,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        DateRange range = DateRange.optional(from, to, tz);
        return movementService.list(productId, type,
                range == null ? null : range.fromInstant(),
                range == null ? null : range.toInstantExclusive(), q,
                PageRequests.of(page, size, sort, SORTABLE,
                        Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un movimiento")
    public StockMovementResponse get(@PathVariable Long id) {
        return movementService.get(id);
    }
}
