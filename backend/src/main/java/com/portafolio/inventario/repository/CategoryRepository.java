package com.portafolio.inventario.repository;

import com.portafolio.inventario.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /** Devuelve [categoryId, cantidad de productos] para todas las categorías con productos. */
    @Query("select p.category.id, count(p) from Product p group by p.category.id")
    List<Object[]> countProductsByCategory();
}
