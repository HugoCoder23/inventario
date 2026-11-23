package com.portafolio.inventario.service;

import com.portafolio.inventario.dto.PageResponse;
import com.portafolio.inventario.dto.ProductRequest;
import com.portafolio.inventario.dto.ProductResponse;
import com.portafolio.inventario.entity.Category;
import com.portafolio.inventario.entity.MovementType;
import com.portafolio.inventario.entity.Product;
import com.portafolio.inventario.exception.ConflictException;
import com.portafolio.inventario.exception.ResourceNotFoundException;
import com.portafolio.inventario.mapper.InventoryMapper;
import com.portafolio.inventario.repository.ProductRepository;
import com.portafolio.inventario.repository.ProductSpecifications;
import com.portafolio.inventario.security.CurrentUserProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final StockMovementService movementService;
    private final CurrentUserProvider currentUser;
    private final InventoryMapper mapper;

    public ProductService(ProductRepository productRepository, CategoryService categoryService,
                          StockMovementService movementService, CurrentUserProvider currentUser,
                          InventoryMapper mapper) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.movementService = movementService;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> list(String q, Long categoryId, Boolean active, Boolean lowStock,
                                              Pageable pageable) {
        return PageResponse.of(
                productRepository.findAll(ProductSpecifications.withFilters(q, categoryId, active, lowStock), pageable),
                mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String sku = normalizeSku(request.sku());
        if (productRepository.existsBySkuIgnoreCase(sku)) {
            throw new ConflictException("DUPLICATE_SKU", "Ya existe un producto con el SKU " + sku);
        }
        Category category = categoryService.find(request.categoryId());
        Product product = new Product(sku, request.name().trim(), category, request.cost(), request.price(),
                request.minStock());
        product.setDescription(CategoryService.blankToNull(request.description()));
        productRepository.save(product);

        if (request.initialStock() != null && request.initialStock() > 0) {
            movementService.apply(product, MovementType.IN, request.initialStock(), null,
                    "Stock inicial", "ALTA-" + sku, currentUser.get(), null);
        }
        return mapper.toResponse(product);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = find(id);
        String sku = normalizeSku(request.sku());
        if (productRepository.existsBySkuIgnoreCaseAndIdNot(sku, id)) {
            throw new ConflictException("DUPLICATE_SKU", "Ya existe un producto con el SKU " + sku);
        }
        product.setSku(sku);
        product.setName(request.name().trim());
        product.setDescription(CategoryService.blankToNull(request.description()));
        product.setCategory(categoryService.find(request.categoryId()));
        product.setCost(request.cost());
        product.setPrice(request.price());
        product.setMinStock(request.minStock());
        // El stock no se edita aquí: solo cambia mediante movimientos.
        return mapper.toResponse(product);
    }

    /** Baja lógica: el producto y su historial se conservan. */
    @Transactional
    public ProductResponse setActive(Long id, boolean active) {
        Product product = find(id);
        product.setActive(active);
        return mapper.toResponse(product);
    }

    private Product find(Long id) {
        return productRepository.findWithCategoryById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));
    }

    static String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }
}
