package com.portafolio.inventario.mapper;

import com.portafolio.inventario.dto.*;
import com.portafolio.inventario.entity.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Conversión entidad → DTO. Se mantiene manual para que sea explícita y sin magia. */
@Component
public class InventoryMapper {

    public ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(), p.getSku(), p.getName(), p.getDescription(),
                p.getCategory().getId(), p.getCategory().getName(),
                p.getCost(), p.getPrice(), p.getMinStock(), p.getStock(),
                p.isLowStock(), p.isActive(),
                p.getCost().multiply(BigDecimal.valueOf(p.getStock())),
                p.getCreatedAt(), p.getUpdatedAt());
    }

    public StockMovementResponse toResponse(StockMovement m) {
        Product p = m.getProduct();
        User u = m.getUser();
        return new StockMovementResponse(
                m.getId(), p.getId(), p.getSku(), p.getName(), m.getType(), m.getQuantity(), m.getDelta(),
                m.getStockBefore(), m.getStockAfter(), m.getReason(), m.getReference(),
                u.getId(), u.getName(), m.getCreatedAt());
    }

    public CategoryResponse toResponse(Category c, long productCount) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), productCount);
    }

    public UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.isActive(), u.getCreatedAt());
    }
}
