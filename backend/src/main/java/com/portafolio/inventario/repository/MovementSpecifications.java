package com.portafolio.inventario.repository;

import com.portafolio.inventario.entity.MovementType;
import com.portafolio.inventario.entity.StockMovement;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.Instant;

public final class MovementSpecifications {

    private MovementSpecifications() {}

    public static Specification<StockMovement> withFilters(Long productId, MovementType type, Instant from,
                                                           Instant to, String q) {
        return Specification.where(forProduct(productId))
                .and(ofType(type))
                .and(from(from))
                .and(to(to))
                .and(search(q));
    }

    static Specification<StockMovement> forProduct(Long productId) {
        if (productId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("product").get("id"), productId);
    }

    static Specification<StockMovement> ofType(MovementType type) {
        if (type == null) return null;
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    static Specification<StockMovement> from(Instant from) {
        if (from == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    static Specification<StockMovement> to(Instant to) {
        if (to == null) return null;
        return (root, query, cb) -> cb.lessThan(root.get("createdAt"), to);
    }

    static Specification<StockMovement> search(String q) {
        if (!StringUtils.hasText(q)) return null;
        String like = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("product").get("sku")), like),
                cb.like(cb.lower(root.get("product").get("name")), like),
                cb.like(cb.lower(root.get("reference")), like),
                cb.like(cb.lower(root.get("reason")), like));
    }
}
