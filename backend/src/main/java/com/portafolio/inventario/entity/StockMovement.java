package com.portafolio.inventario.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Registro inmutable de un cambio de existencias. Nunca se borra ni se edita:
 * los errores se corrigen con un nuevo movimiento de tipo ADJUSTMENT.
 */
@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private MovementType type;

    /** Cantidad positiva indicada por el usuario. */
    @Column(nullable = false, updatable = false)
    private int quantity;

    /** Variación con signo aplicada al stock: positiva entra, negativa sale. */
    @Column(nullable = false, updatable = false)
    private int delta;

    @Column(name = "stock_before", nullable = false, updatable = false)
    private int stockBefore;

    @Column(name = "stock_after", nullable = false, updatable = false)
    private int stockAfter;

    @Column(length = 255, updatable = false)
    private String reason;

    @Column(length = 100, updatable = false)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StockMovement() {}

    public StockMovement(Product product, MovementType type, int quantity, int delta, int stockBefore,
                         int stockAfter, String reason, String reference, User user) {
        this.product = product;
        this.type = type;
        this.quantity = quantity;
        this.delta = delta;
        this.stockBefore = stockBefore;
        this.stockAfter = stockAfter;
        this.reason = reason;
        this.reference = reference;
        this.user = user;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** Permite fijar la fecha al cargar datos históricos de demostración. */
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public MovementType getType() { return type; }
    public int getQuantity() { return quantity; }
    public int getDelta() { return delta; }
    public int getStockBefore() { return stockBefore; }
    public int getStockAfter() { return stockAfter; }
    public String getReason() { return reason; }
    public String getReference() { return reference; }
    public User getUser() { return user; }
    public Instant getCreatedAt() { return createdAt; }
}
