package com.portafolio.inventario.service;

import com.portafolio.inventario.dto.CategoryRequest;
import com.portafolio.inventario.dto.CategoryResponse;
import com.portafolio.inventario.entity.Category;
import com.portafolio.inventario.exception.ConflictException;
import com.portafolio.inventario.exception.ResourceNotFoundException;
import com.portafolio.inventario.mapper.InventoryMapper;
import com.portafolio.inventario.repository.CategoryRepository;
import com.portafolio.inventario.repository.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final InventoryMapper mapper;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository,
                           InventoryMapper mapper) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : categoryRepository.countProductsByCategory()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return categoryRepository.findAll(Sort.by("name")).stream()
                .map(c -> mapper.toResponse(c, counts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("DUPLICATE_CATEGORY", "Ya existe una categoría llamada " + name);
        }
        Category saved = categoryRepository.save(new Category(name, blankToNull(request.description())));
        return mapper.toResponse(saved, 0);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = find(id);
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("DUPLICATE_CATEGORY", "Ya existe una categoría llamada " + name);
        }
        category.setName(name);
        category.setDescription(blankToNull(request.description()));
        long count = productRepository.count((root, q, cb) -> cb.equal(root.get("category").get("id"), id));
        return mapper.toResponse(category, count);
    }

    @Transactional
    public void delete(Long id) {
        Category category = find(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException("CATEGORY_IN_USE",
                    "No se puede eliminar la categoría porque tiene productos asociados");
        }
        categoryRepository.delete(category);
    }

    Category find(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Categoría", id));
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
