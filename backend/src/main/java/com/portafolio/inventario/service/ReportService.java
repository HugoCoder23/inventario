package com.portafolio.inventario.service;

import com.portafolio.inventario.dto.DashboardResponse;
import com.portafolio.inventario.dto.MovementSummaryResponse;
import com.portafolio.inventario.dto.MovementSummaryResponse.DayTotal;
import com.portafolio.inventario.dto.MovementSummaryResponse.ProductTotal;
import com.portafolio.inventario.dto.MovementSummaryResponse.TypeTotal;
import com.portafolio.inventario.dto.ProductResponse;
import com.portafolio.inventario.entity.MovementType;
import com.portafolio.inventario.entity.Product;
import com.portafolio.inventario.entity.StockMovement;
import com.portafolio.inventario.mapper.InventoryMapper;
import com.portafolio.inventario.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ReportService {

    /** Rango máximo de un reporte, para evitar consultas desproporcionadas. */
    static final int MAX_RANGE_DAYS = 366;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StockMovementRepository movementRepository;
    private final InventoryMapper mapper;

    public ReportService(ProductRepository productRepository, CategoryRepository categoryRepository,
                         StockMovementRepository movementRepository, InventoryMapper mapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.movementRepository = movementRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        List<ProductResponse> lowStock = productRepository.findLowStock().stream()
                .limit(5).map(mapper::toResponse).toList();
        return new DashboardResponse(
                productRepository.countActive(),
                productRepository.sumUnits(),
                money(productRepository.inventoryCostValue()),
                money(productRepository.inventoryRetailValue()),
                productRepository.countLowStock(),
                categoryRepository.count(),
                lowStock,
                movementRepository.findTop8ByOrderByCreatedAtDescIdDesc().stream().map(mapper::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> lowStock() {
        return productRepository.findLowStock().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MovementSummaryResponse movementSummary(DateRange range, Long productId, MovementType type) {
        Instant from = range.fromInstant();
        Instant to = range.toInstantExclusive();

        List<TypeTotal> byType = movementRepository.summarizeByType(from, to, productId, type).stream()
                .map(r -> new TypeTotal((MovementType) r[0], num(r[1]), num(r[2]), num(r[3])))
                .toList();

        List<ProductTotal> byProduct = movementRepository.summarizeByProduct(from, to, productId, type).stream()
                .map(r -> new ProductTotal((Long) r[0], (String) r[1], (String) r[2],
                        num(r[3]), num(r[4]), num(r[5]), num(r[6])))
                .toList();

        // Se agrupa por día en Java para respetar la zona horaria del usuario de forma portable entre BD.
        Map<LocalDate, long[]> days = new TreeMap<>();
        for (LocalDate d = range.from(); !d.isAfter(range.to()); d = d.plusDays(1)) {
            days.put(d, new long[2]);
        }
        for (Object[] r : movementRepository.deltasInRange(from, to, productId, type)) {
            LocalDate day = LocalDate.ofInstant((Instant) r[0], range.zone());
            long delta = num(r[1]);
            long[] totals = days.computeIfAbsent(day, k -> new long[2]);
            if (delta > 0) totals[0] += delta; else totals[1] += -delta;
        }
        List<DayTotal> byDay = days.entrySet().stream()
                .map(e -> new DayTotal(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();

        long unitsIn = byProduct.stream().mapToLong(ProductTotal::unitsIn).sum();
        long unitsOut = byProduct.stream().mapToLong(ProductTotal::unitsOut).sum();
        long total = byType.stream().mapToLong(TypeTotal::movements).sum();
        return new MovementSummaryResponse(range.from(), range.to(), total, unitsIn, unitsOut, unitsIn - unitsOut,
                byType, byProduct, byDay);
    }

    @Transactional(readOnly = true)
    public String productsCsv(String q, Long categoryId, Boolean active, Boolean lowStock) {
        List<Product> products = productRepository.findAll(
                ProductSpecifications.withFilters(q, categoryId, active, lowStock), Sort.by("name"));
        CsvWriter csv = new CsvWriter("SKU", "Nombre", "Categoría", "Costo", "Precio", "Stock", "Stock mínimo",
                "Stock bajo", "Valor a costo", "Activo");
        for (Product p : products) {
            csv.row(p.getSku(), p.getName(), p.getCategory().getName(), p.getCost(), p.getPrice(), p.getStock(),
                    p.getMinStock(), p.isLowStock() ? "Sí" : "No",
                    p.getCost().multiply(BigDecimal.valueOf(p.getStock())), p.isActive() ? "Sí" : "No");
        }
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public String movementsCsv(Long productId, MovementType type, DateRange range, String q) {
        List<StockMovement> movements = movementRepository.findAll(
                MovementSpecifications.withFilters(productId, type,
                        range == null ? null : range.fromInstant(),
                        range == null ? null : range.toInstantExclusive(), q),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        ZoneId zone = range == null ? ZoneOffset.UTC : range.zone();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zone);
        CsvWriter csv = new CsvWriter("ID", "Fecha", "SKU", "Producto", "Tipo", "Cantidad", "Variación",
                "Stock anterior", "Stock resultante", "Motivo", "Referencia", "Usuario");
        for (StockMovement m : movements) {
            csv.row(m.getId(), fmt.format(m.getCreatedAt()), m.getProduct().getSku(), m.getProduct().getName(),
                    m.getType(), m.getQuantity(), m.getDelta(), m.getStockBefore(), m.getStockAfter(),
                    m.getReason(), m.getReference(), m.getUser().getName());
        }
        return csv.toString();
    }

    private static long num(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    /** Rango de fechas inclusivo interpretado en la zona horaria indicada. */
    public record DateRange(LocalDate from, LocalDate to, ZoneId zone) {

        public static DateRange of(LocalDate from, LocalDate to, String tz) {
            ZoneId zone;
            try {
                zone = tz == null || tz.isBlank() ? ZoneOffset.UTC : ZoneId.of(tz);
            } catch (DateTimeException e) {
                throw new IllegalArgumentException("Zona horaria inválida: " + tz);
            }
            LocalDate end = to != null ? to : LocalDate.now(zone);
            LocalDate start = from != null ? from : end.minusDays(29);
            if (start.isAfter(end)) {
                throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
            }
            if (start.plusDays(MAX_RANGE_DAYS).isBefore(end)) {
                throw new IllegalArgumentException("El rango máximo de un reporte es de " + MAX_RANGE_DAYS + " días");
            }
            return new DateRange(start, end, zone);
        }

        /** Para listados: si no hay fechas, no se filtra. */
        public static DateRange optional(LocalDate from, LocalDate to, String tz) {
            if (from == null && to == null) return null;
            ZoneId zone = of(LocalDate.now(), LocalDate.now(), tz).zone();
            LocalDate start = from != null ? from : LocalDate.of(2000, 1, 1);
            LocalDate end = to != null ? to : LocalDate.now(zone).plusDays(1);
            if (start.isAfter(end)) {
                throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
            }
            return new DateRange(start, end, zone);
        }

        public Instant fromInstant() {
            return from.atStartOfDay(zone).toInstant();
        }

        public Instant toInstantExclusive() {
            return to.plusDays(1).atStartOfDay(zone).toInstant();
        }
    }
}
