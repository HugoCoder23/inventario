package com.portafolio.inventario.controller;

import com.portafolio.inventario.dto.PageResponse;
import com.portafolio.inventario.dto.ProductRequest;
import com.portafolio.inventario.dto.ProductResponse;
import com.portafolio.inventario.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Productos")
public class ProductController {

    static final Map<String, String> SORTABLE = new LinkedHashMap<>();
    static {
        SORTABLE.put("sku", "sku");
        SORTABLE.put("name", "name");
        SORTABLE.put("category", "category.name");
        SORTABLE.put("stock", "stock");
        SORTABLE.put("minStock", "minStock");
        SORTABLE.put("cost", "cost");
        SORTABLE.put("price", "price");
        SORTABLE.put("createdAt", "createdAt");
        SORTABLE.put("updatedAt", "updatedAt");
    }

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Listar productos con búsqueda, filtros, ordenamiento y paginación",
            description = "sort admite: sku, name, category, stock, minStock, cost, price, createdAt, updatedAt (p. ej. name,asc)")
    public PageResponse<ProductResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        return productService.list(q, categoryId, active, lowStock,
                PageRequests.of(page, size, sort, SORTABLE, Sort.by("name").and(Sort.by("id"))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un producto")
    public ProductResponse get(@PathVariable Long id) {
        return productService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    @Operation(summary = "Crear producto (opcionalmente con stock inicial)")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    @Operation(summary = "Modificar producto (el stock solo cambia mediante movimientos)")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Desactivar producto (baja lógica, conserva historial)")
    public ProductResponse deactivate(@PathVariable Long id) {
        return productService.setActive(id, false);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reactivar producto")
    public ProductResponse activate(@PathVariable Long id) {
        return productService.setActive(id, true);
    }
}
