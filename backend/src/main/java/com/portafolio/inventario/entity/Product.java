package com.portafolio.inventario.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String sku;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "min_stock", nullable = false)
    private int minStock;

    /**
     * Existencias actuales. Solo cambia a través de StockMovementService,
     * siempre en la misma transacción que el registro del movimiento.
     */
    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    private long version;

    protected Product() {}

    public Product(String sku, String name, Category category, BigDecimal cost, BigDecimal price, int minStock) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.cost = cost;
        this.price = price;
        this.minStock = minStock;
    }

    public boolean isLowStock() {
        return stock <= minStock;
    }

    /** Aplica una variación de stock garantizando que el resultado no sea negativo. */
    public void applyStockDelta(int delta) {
        int result = this.stock + delta;
        if (result < 0) {
            throw new IllegalStateException("El stock no puede quedar negativo");
        }
        this.stock = result;
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getMinStock() { return minStock; }
    public void setMinStock(int minStock) { this.minStock = minStock; }
    public int getStock() { return stock; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public long getVersion() { return version; }
}
