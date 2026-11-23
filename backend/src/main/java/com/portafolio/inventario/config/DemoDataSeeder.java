package com.portafolio.inventario.config;

import com.portafolio.inventario.dto.AdjustmentDirection;
import com.portafolio.inventario.entity.*;
import com.portafolio.inventario.repository.CategoryRepository;
import com.portafolio.inventario.repository.ProductRepository;
import com.portafolio.inventario.repository.UserRepository;
import com.portafolio.inventario.service.StockMovementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Carga un dataset ficticio para la demo cuando la base está vacía. Todo el stock se genera
 * mediante movimientos (no se escribe directamente), así el historial cuadra con las existencias.
 */
@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final StockMovementService movementService;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    public DemoDataSeeder(UserRepository userRepository, CategoryRepository categoryRepository,
                          ProductRepository productRepository, StockMovementService movementService,
                          PasswordEncoder passwordEncoder, AppProperties props) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.movementService = movementService;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    private record Seed(String sku, String name, String category, String cost, String price, int min, int initial,
                        String description) {}

    private static final List<Seed> PRODUCTS = List.of(
            new Seed("ELE-LAP-001", "Laptop Dell Latitude 5440", "Electrónicos", "14500.00", "18999.00", 5, 24, "Intel Core i5, 16 GB RAM, SSD 512 GB"),
            new Seed("ELE-MON-002", "Monitor LG 27\" IPS", "Electrónicos", "3200.00", "4499.00", 8, 30, "Resolución QHD, 75 Hz"),
            new Seed("ELE-TAB-003", "Tablet Samsung Galaxy Tab A9", "Electrónicos", "2900.00", "3999.00", 6, 18, "Pantalla 8.7\", 64 GB"),
            new Seed("ELE-PRO-004", "Proyector Epson PowerLite", "Electrónicos", "8900.00", "11500.00", 2, 6, "3LCD, 3600 lúmenes"),
            new Seed("ACC-MOU-001", "Mouse Logitech M185", "Accesorios", "180.00", "299.00", 20, 120, "Inalámbrico, receptor USB"),
            new Seed("ACC-TEC-002", "Teclado mecánico Redragon Kumara", "Accesorios", "650.00", "999.00", 10, 45, "Switches rojos, distribución en español"),
            new Seed("ACC-AUD-003", "Audífonos HyperX Cloud Stinger", "Accesorios", "720.00", "1099.00", 8, 32, "Con micrófono, conector 3.5 mm"),
            new Seed("ACC-WEB-004", "Cámara web Logitech C920", "Accesorios", "890.00", "1349.00", 6, 20, "Full HD 1080p"),
            new Seed("ACC-HUB-005", "Hub USB-C 7 en 1", "Accesorios", "310.00", "549.00", 12, 40, "HDMI 4K, lector SD, 3 USB-A"),
            new Seed("RED-ROU-001", "Router TP-Link Archer C6", "Redes", "520.00", "799.00", 5, 25, "AC1200 doble banda"),
            new Seed("RED-SWI-002", "Switch TP-Link 8 puertos Gigabit", "Redes", "340.00", "529.00", 5, 16, "No administrable"),
            new Seed("RED-CAB-003", "Cable UTP Cat6 3 m", "Redes", "35.00", "69.00", 50, 300, "Patch cord azul"),
            new Seed("ALM-SSD-001", "SSD Kingston NV2 1 TB", "Almacenamiento", "980.00", "1399.00", 10, 50, "NVMe PCIe 4.0"),
            new Seed("ALM-USB-002", "Memoria USB SanDisk 64 GB", "Almacenamiento", "95.00", "169.00", 25, 140, "USB 3.0"),
            new Seed("ALM-HDD-003", "Disco externo Seagate 2 TB", "Almacenamiento", "1150.00", "1599.00", 6, 22, "USB 3.0, portátil"),
            new Seed("OFI-SIL-001", "Silla ergonómica de malla", "Oficina", "1800.00", "2799.00", 4, 15, "Soporte lumbar ajustable"),
            new Seed("OFI-PAP-002", "Papel bond carta (caja 5000 hojas)", "Oficina", "420.00", "599.00", 10, 60, "75 g/m²"),
            new Seed("OFI-TON-003", "Tóner HP 26A", "Oficina", "1350.00", "1899.00", 5, 14, "Rendimiento 3100 páginas"),
            new Seed("OFI-ESC-004", "Escritorio de melamina 120 cm", "Oficina", "1450.00", "2199.00", 3, 8, "Color nogal"));

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        String password = passwordEncoder.encode(props.seed().demoPassword());
        User admin = userRepository.save(new User("Ana Administradora", "admin@demo.local", password, Role.ADMIN));
        User warehouse = userRepository.save(new User("Carlos Almacén", "almacen@demo.local", password, Role.WAREHOUSE));
        userRepository.save(new User("Valeria Consulta", "consulta@demo.local", password, Role.VIEWER));

        Map<String, Category> categories = new LinkedHashMap<>();
        for (String[] c : new String[][]{
                {"Electrónicos", "Equipos de cómputo y dispositivos"},
                {"Accesorios", "Periféricos y complementos"},
                {"Redes", "Conectividad y cableado"},
                {"Almacenamiento", "Discos, memorias y unidades"},
                {"Oficina", "Mobiliario y consumibles"}}) {
            categories.put(c[0], categoryRepository.save(new Category(c[0], c[1])));
        }

        Random random = new Random(42);
        Instant now = Instant.now();
        Set<String> forceLow = Set.of("ELE-PRO-004", "ACC-WEB-004", "OFI-TON-003", "RED-SWI-002");

        for (Seed s : PRODUCTS) {
            Product p = new Product(s.sku(), s.name(), categories.get(s.category()),
                    new BigDecimal(s.cost()), new BigDecimal(s.price()), s.min());
            p.setDescription(s.description());
            productRepository.save(p);

            Instant t = now.minus(Duration.ofDays(60)).plus(random.nextInt(48), ChronoUnit.HOURS);
            movementService.apply(p, MovementType.IN, s.initial(), null, "Stock inicial", "OC-1000", warehouse, t);

            while (true) {
                t = t.plus(Duration.ofHours(48 + random.nextInt(120)));
                if (t.isAfter(now.minus(Duration.ofHours(2)))) break;
                boolean exit = random.nextDouble() < 0.7;
                if (exit && p.getStock() > 1) {
                    int qty = 1 + random.nextInt(Math.max(1, p.getStock() / 4));
                    movementService.apply(p, MovementType.OUT, qty, null, "Venta a cliente",
                            "FAC-" + (2000 + random.nextInt(8000)), warehouse, t);
                } else {
                    int qty = Math.max(1, s.initial() / 3 + random.nextInt(Math.max(1, s.initial() / 4)));
                    movementService.apply(p, MovementType.IN, qty, null, "Reabastecimiento de proveedor",
                            "OC-" + (1000 + random.nextInt(900)), warehouse, t);
                }
            }

            if (forceLow.contains(s.sku()) && p.getStock() > s.min()) {
                int qty = p.getStock() - Math.max(0, s.min() - 1 - random.nextInt(2));
                movementService.apply(p, MovementType.OUT, qty, null, "Pedido mayorista",
                        "FAC-" + (2000 + random.nextInt(8000)), warehouse, now.minus(Duration.ofHours(5)));
            }
        }

        // Un par de ajustes por conteo físico para ilustrar la trazabilidad.
        productRepository.findAll().stream().filter(p -> p.getSku().equals("ACC-MOU-001")).findFirst()
                .ifPresent(p -> movementService.apply(p, MovementType.ADJUSTMENT, 2, AdjustmentDirection.DECREASE,
                        "Conteo físico: 2 piezas dañadas", "INV-2026-01", admin, now.minus(Duration.ofHours(30))));
        productRepository.findAll().stream().filter(p -> p.getSku().equals("RED-CAB-003")).findFirst()
                .ifPresent(p -> movementService.apply(p, MovementType.ADJUSTMENT, 5, AdjustmentDirection.INCREASE,
                        "Conteo físico: sobrante no registrado", "INV-2026-01", admin, now.minus(Duration.ofHours(29))));

        log.info("Datos demo cargados: {} productos. Cuentas: admin@demo.local, almacen@demo.local, consulta@demo.local",
                PRODUCTS.size());
    }
}
