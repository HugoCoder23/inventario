package com.portafolio.inventario.repository;

import com.portafolio.inventario.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);
    boolean existsByCategoryId(Long categoryId);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findWithCategoryById(Long id);

    /**
     * Bloquea la fila del producto hasta el final de la transacción para que dos
     * movimientos simultáneos no lean el mismo stock y generen un saldo incorrecto.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = "category")
    @Query("select p from Product p where p.active = true and p.stock <= p.minStock order by (p.stock - p.minStock), p.name")
    List<Product> findLowStock();

    @Query("select count(p) from Product p where p.active = true")
    long countActive();

    @Query("select count(p) from Product p where p.active = true and p.stock <= p.minStock")
    long countLowStock();

    @Query("select coalesce(sum(p.stock), 0) from Product p where p.active = true")
    long sumUnits();

    @Query("select coalesce(sum(p.cost * p.stock), 0) from Product p where p.active = true")
    BigDecimal inventoryCostValue();

    @Query("select coalesce(sum(p.price * p.stock), 0) from Product p where p.active = true")
    BigDecimal inventoryRetailValue();
}
