package com.portafolio.inventario.service;

import com.portafolio.inventario.dto.AdjustmentDirection;
import com.portafolio.inventario.dto.PageResponse;
import com.portafolio.inventario.dto.StockMovementRequest;
import com.portafolio.inventario.dto.StockMovementResponse;
import com.portafolio.inventario.entity.MovementType;
import com.portafolio.inventario.entity.Product;
import com.portafolio.inventario.entity.StockMovement;
import com.portafolio.inventario.entity.User;
import com.portafolio.inventario.exception.BusinessRuleException;
import com.portafolio.inventario.exception.ResourceNotFoundException;
import com.portafolio.inventario.mapper.InventoryMapper;
import com.portafolio.inventario.repository.MovementSpecifications;
import com.portafolio.inventario.repository.ProductRepository;
import com.portafolio.inventario.repository.StockMovementRepository;
import com.portafolio.inventario.security.CurrentUserProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

/**
 * Único punto del sistema que modifica existencias. El cambio de stock y el registro
 * del movimiento ocurren en la misma transacción: si cualquiera falla, no se guarda nada.
 */
@Service
public class StockMovementService {

    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;
    private final CurrentUserProvider currentUser;
    private final InventoryMapper mapper;

    public StockMovementService(ProductRepository productRepository, StockMovementRepository movementRepository,
                                CurrentUserProvider currentUser, InventoryMapper mapper) {
        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Transactional
    public StockMovementResponse register(StockMovementRequest request) {
        User user = currentUser.get();
        // Bloqueo pesimista: serializa movimientos concurrentes sobre el mismo producto.
        Product product = productRepository.findByIdForUpdate(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto", request.productId()));
        if (!product.isActive()) {
            throw new BusinessRuleException("PRODUCT_INACTIVE",
                    "No se pueden registrar movimientos sobre un producto desactivado");
        }
        StockMovement movement = apply(product, request.type(), request.quantity(), request.direction(),
                request.reason(), request.reference(), user, null);
        return mapper.toResponse(movement);
    }

    /**
     * Aplica un movimiento sobre un producto ya cargado (y bloqueado) dentro de una transacción activa.
     * Repite en servidor todas las validaciones críticas, aunque el DTO ya se haya validado.
     */
    @Transactional
    public StockMovement apply(Product product, MovementType type, Integer quantity, AdjustmentDirection direction,
                               String reason, String reference, User user, Instant occurredAt) {
        if (type == null) {
            throw new BusinessRuleException("INVALID_TYPE", "El tipo de movimiento es obligatorio");
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessRuleException("INVALID_QUANTITY", "La cantidad debe ser mayor que cero");
        }
        String cleanReason = StringUtils.hasText(reason) ? reason.trim() : null;
        if (type == MovementType.ADJUSTMENT) {
            if (direction == null) {
                throw new BusinessRuleException("ADJUSTMENT_DIRECTION_REQUIRED",
                        "Indica si el ajuste aumenta o disminuye el stock");
            }
            if (cleanReason == null) {
                throw new BusinessRuleException("ADJUSTMENT_REASON_REQUIRED", "Los ajustes deben explicar el motivo");
            }
        }

        int delta = switch (type) {
            case IN -> quantity;
            case OUT -> -quantity;
            case ADJUSTMENT -> direction == AdjustmentDirection.DECREASE ? -quantity : quantity;
        };

        int before = product.getStock();
        if (before + delta < 0) {
            throw new BusinessRuleException("INSUFFICIENT_STOCK", String.format(
                    "Stock insuficiente para %s: disponible %d, solicitado %d", product.getSku(), before, quantity));
        }
        product.applyStockDelta(delta);

        StockMovement movement = new StockMovement(product, type, quantity, delta, before, product.getStock(),
                cleanReason, StringUtils.hasText(reference) ? reference.trim() : null, user);
        if (occurredAt != null) {
            movement.setCreatedAt(occurredAt);
        }
        return movementRepository.save(movement);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> list(Long productId, MovementType type, Instant from, Instant to,
                                                    String q, Pageable pageable) {
        return PageResponse.of(
                movementRepository.findAll(MovementSpecifications.withFilters(productId, type, from, to, q), pageable),
                mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public StockMovementResponse get(Long id) {
        return movementRepository.findById(id).map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento", id));
    }
}
