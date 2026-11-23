package com.portafolio.inventario.repository;

import com.portafolio.inventario.entity.Product;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProductSpecifications {

    private ProductSpecifications() {}

    public static Specification<Product> withFilters(String q, Long categoryId, Boolean active, Boolean lowStock) {
        return Specification.where(search(q))
                .and(inCategory(categoryId))
                .and(isActive(active))
                .and(lowStock(lowStock));
    }

    static Specification<Product> search(String q) {
        if (!StringUtils.hasText(q)) return null;
        String like = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("sku")), like),
                cb.like(cb.lower(root.get("name")), like),
                cb.like(cb.lower(root.get("description")), like));
    }

    static Specification<Product> inCategory(Long categoryId) {
        if (categoryId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    static Specification<Product> isActive(Boolean active) {
        if (active == null) return null;
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    static Specification<Product> lowStock(Boolean lowStock) {
        if (lowStock == null) return null;
        return (root, query, cb) -> lowStock
                ? cb.lessThanOrEqualTo(root.get("stock"), root.<Integer>get("minStock"))
                : cb.greaterThan(root.get("stock"), root.<Integer>get("minStock"));
    }
}
