package com.portafolio.inventario.repository;

import com.portafolio.inventario.entity.MovementType;
import com.portafolio.inventario.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement> {

    @Override
    @EntityGraph(attributePaths = {"product", "user"})
    Page<StockMovement> findAll(Specification<StockMovement> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"product", "user"})
    List<StockMovement> findAll(Specification<StockMovement> spec, Sort sort);

    @EntityGraph(attributePaths = {"product", "user"})
    List<StockMovement> findTop8ByOrderByCreatedAtDescIdDesc();

    /** [type, movimientos, unidades, variación neta] por tipo de movimiento. */
    @Query("""
            select m.type, count(m), coalesce(sum(m.quantity), 0), coalesce(sum(m.delta), 0)
            from StockMovement m
            where m.createdAt >= :from and m.createdAt < :to
              and (:productId is null or m.product.id = :productId)
              and (:type is null or m.type = :type)
            group by m.type
            """)
    List<Object[]> summarizeByType(@Param("from") Instant from, @Param("to") Instant to,
                                   @Param("productId") Long productId, @Param("type") MovementType type);

    /** [productId, sku, nombre, movimientos, unidades que entran, unidades que salen, variación neta]. */
    @Query("""
            select p.id, p.sku, p.name, count(m),
                   coalesce(sum(case when m.delta > 0 then m.delta else 0 end), 0),
                   coalesce(sum(case when m.delta < 0 then -m.delta else 0 end), 0),
                   coalesce(sum(m.delta), 0)
            from StockMovement m join m.product p
            where m.createdAt >= :from and m.createdAt < :to
              and (:productId is null or p.id = :productId)
              and (:type is null or m.type = :type)
            group by p.id, p.sku, p.name
            order by count(m) desc, p.name
            """)
    List<Object[]> summarizeByProduct(@Param("from") Instant from, @Param("to") Instant to,
                                      @Param("productId") Long productId, @Param("type") MovementType type);

    /** [createdAt, delta] para agrupar por día en la zona horaria del usuario. */
    @Query("""
            select m.createdAt, m.delta
            from StockMovement m
            where m.createdAt >= :from and m.createdAt < :to
              and (:productId is null or m.product.id = :productId)
              and (:type is null or m.type = :type)
            """)
    List<Object[]> deltasInRange(@Param("from") Instant from, @Param("to") Instant to,
                                 @Param("productId") Long productId, @Param("type") MovementType type);
}
