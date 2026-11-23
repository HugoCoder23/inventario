package com.portafolio.inventario.service;

import com.portafolio.inventario.dto.AdjustmentDirection;
import com.portafolio.inventario.dto.StockMovementRequest;
import com.portafolio.inventario.entity.*;
import com.portafolio.inventario.exception.BusinessRuleException;
import com.portafolio.inventario.exception.ResourceNotFoundException;
import com.portafolio.inventario.mapper.InventoryMapper;
import com.portafolio.inventario.repository.ProductRepository;
import com.portafolio.inventario.repository.StockMovementRepository;
import com.portafolio.inventario.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock ProductRepository productRepository;
    @Mock StockMovementRepository movementRepository;
    @Mock CurrentUserProvider currentUser;
    @Spy InventoryMapper mapper = new InventoryMapper();
    @InjectMocks StockMovementService service;

    Product product;
    User user;

    @BeforeEach
    void setUp() {
        Category category = new Category("Accesorios", null);
        ReflectionTestUtils.setField(category, "id", 1L);
        product = new Product("SKU-1", "Mouse", category, new BigDecimal("10.00"), new BigDecimal("20.00"), 2);
        ReflectionTestUtils.setField(product, "id", 7L);
        user = new User("Tester", "t@demo.local", "hash", Role.WAREHOUSE);
        ReflectionTestUtils.setField(user, "id", 3L);
    }

    private void givenStock(int stock) {
        ReflectionTestUtils.setField(product, "stock", stock);
    }

    @Test
    void entradaAumentaElStockYRegistraElSaldo() {
        givenStock(5);
        when(movementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StockMovement m = service.apply(product, MovementType.IN, 3, null, null, "OC-1", user, null);

        assertThat(product.getStock()).isEqualTo(8);
        assertThat(m.getDelta()).isEqualTo(3);
        assertThat(m.getStockBefore()).isEqualTo(5);
        assertThat(m.getStockAfter()).isEqualTo(8);
    }

    @Test
    void salidaDisminuyeElStock() {
        givenStock(5);
        when(movementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StockMovement m = service.apply(product, MovementType.OUT, 5, null, null, null, user, null);

        assertThat(product.getStock()).isZero();
        assertThat(m.getDelta()).isEqualTo(-5);
    }

    @Test
    void salidaMayorAlStockSeRechazaSinModificarNada() {
        givenStock(4);

        assertThatThrownBy(() -> service.apply(product, MovementType.OUT, 5, null, null, null, user, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Stock insuficiente");

        assertThat(product.getStock()).isEqualTo(4);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void ajusteRequiereSentidoYMotivo() {
        givenStock(4);
        assertThatThrownBy(() -> service.apply(product, MovementType.ADJUSTMENT, 1, null, "Conteo", null, user, null))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.apply(product, MovementType.ADJUSTMENT, 1, AdjustmentDirection.DECREASE,
                "  ", null, user, null))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(product.getStock()).isEqualTo(4);
    }

    @Test
    void ajusteNegativoDescuentaStock() {
        givenStock(4);
        when(movementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StockMovement m = service.apply(product, MovementType.ADJUSTMENT, 3, AdjustmentDirection.DECREASE,
                "Conteo físico", null, user, null);

        assertThat(product.getStock()).isEqualTo(1);
        assertThat(m.getDelta()).isEqualTo(-3);
    }

    @Test
    void cantidadCeroONegativaSeRechazaEnServidor() {
        givenStock(4);
        assertThatThrownBy(() -> service.apply(product, MovementType.IN, 0, null, null, null, user, null))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.apply(product, MovementType.IN, -2, null, null, null, user, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void noSePuedeMoverUnProductoDesactivado() {
        product.setActive(false);
        when(currentUser.get()).thenReturn(user);
        when(productRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.register(
                new StockMovementRequest(7L, MovementType.IN, 1, null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("desactivado");
    }

    @Test
    void productoInexistenteDevuelveNoEncontrado() {
        when(currentUser.get()).thenReturn(user);
        when(productRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(
                new StockMovementRequest(99L, MovementType.IN, 1, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
