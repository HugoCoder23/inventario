package com.portafolio.inventario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portafolio.inventario.entity.Category;
import com.portafolio.inventario.entity.Role;
import com.portafolio.inventario.entity.User;
import com.portafolio.inventario.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Pruebas de aceptación de la guía, ejecutadas de punta a punta contra la API (H2 en memoria). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryApiIntegrationTest {

    static final String PASSWORD = "Password123";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @SpyBean StockMovementRepository movements;
    @Autowired PasswordEncoder encoder;

    String adminToken;
    String warehouseToken;
    String viewerToken;
    Long categoryId;

    @BeforeEach
    void setUp() throws Exception {
        reset(movements);
        movements.deleteAllInBatch();
        products.deleteAllInBatch();
        categories.deleteAllInBatch();
        users.deleteAllInBatch();

        users.save(new User("Admin", "admin@test.local", encoder.encode(PASSWORD), Role.ADMIN));
        users.save(new User("Almacén", "almacen@test.local", encoder.encode(PASSWORD), Role.WAREHOUSE));
        users.save(new User("Consulta", "consulta@test.local", encoder.encode(PASSWORD), Role.VIEWER));
        categoryId = categories.save(new Category("Accesorios", null)).getId();

        adminToken = login("admin@test.local");
        warehouseToken = login("almacen@test.local");
        viewerToken = login("consulta@test.local");
    }

    // ---------- helpers ----------

    String login(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    ResultActions postJson(String url, String token, Object body) throws Exception {
        return mvc.perform(post(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    ResultActions getWith(String url, String token) throws Exception {
        return mvc.perform(get(url).header("Authorization", "Bearer " + token));
    }

    Map<String, Object> product(String sku, int initialStock) {
        return Map.of("sku", sku, "name", "Producto " + sku, "categoryId", categoryId,
                "cost", 10.50, "price", 19.99, "minStock", 5, "initialStock", initialStock);
    }

    long createProduct(String sku, int initialStock) throws Exception {
        String body = postJson("/api/products", adminToken, product(sku, initialStock))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    int stockOf(long productId) throws Exception {
        String body = getWith("/api/products/" + productId, viewerToken).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("stock").asInt();
    }

    // ---------- autenticación y roles ----------

    @Test
    @DisplayName("Login inválido devuelve 401 con formato de error uniforme")
    void loginInvalido() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.local\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    @DisplayName("Sin token se rechaza con 401; con token manipulado también")
    void accesoNoAutorizado() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        getWith("/api/products", adminToken + "x").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El rol consulta puede leer pero no crear productos ni movimientos")
    void rolConsultaSoloLectura() throws Exception {
        getWith("/api/products", viewerToken).andExpect(status().isOk());
        postJson("/api/products", viewerToken, product("VIEW-1", 0))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        long id = createProduct("VIEW-2", 5);
        postJson("/api/stock-movements", viewerToken, Map.of("productId", id, "type", "IN", "quantity", 1))
                .andExpect(status().isForbidden());
        getWith("/api/users", viewerToken).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Solo el administrador desactiva productos")
    void soloAdminDesactiva() throws Exception {
        long id = createProduct("DEL-1", 0);
        mvc.perform(delete("/api/products/" + id).header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/products/" + id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    // ---------- productos ----------

    @Test
    @DisplayName("Crear producto válido y rechazar SKU duplicado (sin importar mayúsculas)")
    void skuDuplicado() throws Exception {
        postJson("/api/products", adminToken, product("abc-001", 0))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.sku").value("ABC-001"))
                .andExpect(jsonPath("$.stock").value(0));

        postJson("/api/products", adminToken, product("ABC-001", 0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SKU"));
    }

    @Test
    @DisplayName("Validaciones del producto en servidor devuelven 400 con detalle por campo")
    void productoInvalido() throws Exception {
        postJson("/api/products", adminToken, Map.of("sku", "", "name", "X", "categoryId", categoryId,
                "cost", -1, "price", 5, "minStock", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItems("sku", "cost")));
    }

    @Test
    @DisplayName("El stock inicial se registra como movimiento de entrada")
    void stockInicialGeneraMovimiento() throws Exception {
        long id = createProduct("INI-1", 12);
        org.assertj.core.api.Assertions.assertThat(stockOf(id)).isEqualTo(12);
        getWith("/api/stock-movements?productId=" + id, viewerToken)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("IN"))
                .andExpect(jsonPath("$.content[0].reason").value("Stock inicial"));
    }

    @Test
    @DisplayName("Filtros, búsqueda, ordenamiento y paginación de productos")
    void filtrosYPaginacion() throws Exception {
        for (int i = 1; i <= 12; i++) {
            createProduct(String.format("PAG-%02d", i), i <= 3 ? 1 : 20); // 3 quedan bajo el mínimo (5)
        }
        getWith("/api/products?page=1&size=5&sort=sku,asc", viewerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.content[0].sku").value("PAG-06"));

        getWith("/api/products?lowStock=true", viewerToken)
                .andExpect(jsonPath("$.totalElements").value(3));
        getWith("/api/products?q=pag-1", viewerToken)
                .andExpect(jsonPath("$.totalElements").value(3)); // PAG-10, 11, 12
        getWith("/api/products?sort=stock,desc&size=1", viewerToken)
                .andExpect(jsonPath("$.content[0].stock").value(20));
        getWith("/api/products?sort=password", viewerToken)
                .andExpect(status().isBadRequest());
        getWith("/api/products?size=1000", viewerToken)
                .andExpect(status().isBadRequest());
    }

    // ---------- movimientos ----------

    @Test
    @DisplayName("Entrada aumenta y salida disminuye el stock")
    void entradaYSalida() throws Exception {
        long id = createProduct("MOV-1", 10);

        postJson("/api/stock-movements", warehouseToken,
                Map.of("productId", id, "type", "IN", "quantity", 5, "reference", "OC-77"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockBefore").value(10))
                .andExpect(jsonPath("$.stockAfter").value(15))
                .andExpect(jsonPath("$.userName").value("Almacén"));
        org.assertj.core.api.Assertions.assertThat(stockOf(id)).isEqualTo(15);

        postJson("/api/stock-movements", warehouseToken, Map.of("productId", id, "type", "OUT", "quantity", 15))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.delta").value(-15))
                .andExpect(jsonPath("$.stockAfter").value(0));
        org.assertj.core.api.Assertions.assertThat(stockOf(id)).isZero();
    }

    @Test
    @DisplayName("Una salida mayor al stock se rechaza con 422 y no altera nada")
    void salidaMayorAlStock() throws Exception {
        long id = createProduct("MOV-2", 3);

        postJson("/api/stock-movements", warehouseToken, Map.of("productId", id, "type", "OUT", "quantity", 4))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        org.assertj.core.api.Assertions.assertThat(stockOf(id)).isEqualTo(3);
        getWith("/api/stock-movements?productId=" + id, viewerToken)
                .andExpect(jsonPath("$.totalElements").value(1)); // solo el stock inicial
    }

    @Test
    @DisplayName("Si falla el guardado del movimiento, el stock no queda modificado (rollback)")
    void movimientoFallidoHaceRollback() throws Exception {
        long id = createProduct("MOV-3", 10);
        doThrow(new IllegalStateException("Fallo simulado de base de datos")).when(movements).save(any());

        postJson("/api/stock-movements", warehouseToken, Map.of("productId", id, "type", "OUT", "quantity", 4))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));

        reset(movements);
        org.assertj.core.api.Assertions.assertThat(stockOf(id)).isEqualTo(10);
    }

    @Test
    @DisplayName("Cantidad debe ser mayor que cero y los ajustes deben explicar el motivo")
    void validacionesDeMovimiento() throws Exception {
        long id = createProduct("MOV-4", 10);

        postJson("/api/stock-movements", warehouseToken, Map.of("productId", id, "type", "IN", "quantity", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("quantity"));

        postJson("/api/stock-movements", warehouseToken,
                Map.of("productId", id, "type", "ADJUSTMENT", "quantity", 2, "direction", "DECREASE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("reason")));

        postJson("/api/stock-movements", warehouseToken, Map.of("productId", id, "type", "ADJUSTMENT",
                        "quantity", 2, "direction", "DECREASE", "reason", "Conteo físico: merma"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockAfter").value(8));
    }

    @Test
    @DisplayName("Los movimientos históricos no se pueden borrar ni editar por la API")
    void movimientosInmutables() throws Exception {
        long id = createProduct("MOV-5", 1);
        String body = getWith("/api/stock-movements?productId=" + id, viewerToken)
                .andReturn().getResponse().getContentAsString();
        long movementId = json.readTree(body).at("/content/0/id").asLong();
        mvc.perform(delete("/api/stock-movements/" + movementId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(put("/api/stock-movements/" + movementId).header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("Movimiento sobre producto inexistente devuelve 404")
    void productoInexistente() throws Exception {
        postJson("/api/stock-movements", warehouseToken, Map.of("productId", 999999, "type", "IN", "quantity", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // ---------- reportes ----------

    @Test
    @DisplayName("Alertas de stock bajo, dashboard, resumen y exportación CSV")
    void reportes() throws Exception {
        long low = createProduct("REP-LOW", 2);   // mínimo 5 → bajo
        long ok = createProduct("REP-OK", 50);
        postJson("/api/stock-movements", warehouseToken, Map.of("productId", ok, "type", "OUT", "quantity", 8))
                .andExpect(status().isCreated());

        getWith("/api/reports/low-stock", viewerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(low));

        getWith("/api/dashboard", viewerToken)
                .andExpect(jsonPath("$.totalProducts").value(2))
                .andExpect(jsonPath("$.totalUnits").value(44))
                .andExpect(jsonPath("$.lowStockCount").value(1))
                .andExpect(jsonPath("$.inventoryCostValue").value(462.0)) // 44 × 10.50
                .andExpect(jsonPath("$.recentMovements", hasSize(3)));

        String summary = getWith("/api/reports/movements?tz=America/Mexico_City", viewerToken)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode s = json.readTree(summary);
        org.assertj.core.api.Assertions.assertThat(s.get("totalMovements").asLong()).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(s.get("unitsIn").asLong()).isEqualTo(52);
        org.assertj.core.api.Assertions.assertThat(s.get("unitsOut").asLong()).isEqualTo(8);
        org.assertj.core.api.Assertions.assertThat(s.get("byDay")).hasSize(30);

        getWith("/api/reports/movements?type=OUT", viewerToken)
                .andExpect(jsonPath("$.totalMovements").value(1));
        getWith("/api/reports/movements?from=2026-02-10&to=2026-01-01", viewerToken)
                .andExpect(status().isBadRequest());

        getWith("/api/reports/products/export", viewerToken)
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("productos.csv")))
                .andExpect(content().string(containsString("REP-LOW")));
        getWith("/api/reports/movements/export?type=OUT", viewerToken)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("REP-OK")))
                .andExpect(content().string(not(containsString("REP-LOW"))));
    }

    // ---------- categorías y usuarios ----------

    @Test
    @DisplayName("No se puede eliminar una categoría con productos ni duplicar su nombre")
    void categorias() throws Exception {
        createProduct("CAT-1", 0);
        mvc.perform(delete("/api/categories/" + categoryId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_IN_USE"));
        postJson("/api/categories", adminToken, Map.of("name", "accesorios"))
                .andExpect(status().isConflict());
        postJson("/api/categories", warehouseToken, Map.of("name", "Nueva"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El administrador gestiona usuarios y un usuario desactivado no puede iniciar sesión")
    void usuarios() throws Exception {
        String body = postJson("/api/users", adminToken, Map.of("name", "Nuevo", "email", "nuevo@test.local",
                        "password", "Segura123", "role", "WAREHOUSE"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(body).get("id").asLong();

        mvc.perform(put("/api/users/" + id).header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "Nuevo", "email", "nuevo@test.local",
                                "role", "WAREHOUSE", "active", false))))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nuevo@test.local\",\"password\":\"Segura123\"}"))
                .andExpect(status().isUnauthorized());
    }
}
