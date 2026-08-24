package com.ecommerce.orderservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "merchant_id", nullable = false, updatable = false)
    private UUID merchantId;

    @Column(name = "product_name_snapshot", nullable = false, length = 255, updatable = false)
    private String productNameSnapshot;

    @Column(name = "quantity", nullable = false, updatable = false)
    private int quantity;

    @Column(name = "unit_price_paid", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal unitPricePaid;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrderItem() {
    }

    public OrderItem(UUID productId, UUID merchantId, String productNameSnapshot,
                     int quantity, BigDecimal unitPricePaid) {
        this.productId = productId;
        this.merchantId = merchantId;
        this.productNameSnapshot = productNameSnapshot;
        this.quantity = quantity;
        this.unitPricePaid = unitPricePaid;
    }

    void setOrder(Order order) {
        this.order = order;
    }

    public BigDecimal lineTotal() {
        return unitPricePaid.multiply(BigDecimal.valueOf(quantity));
    }

    public UUID getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public UUID getProductId() {
        return productId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPricePaid() {
        return unitPricePaid;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof OrderItem orderItem)) {
            return false;
        }
        return id != null && Objects.equals(id, orderItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "OrderItem{id=" + id + ", productId=" + productId + ", quantity=" + quantity
                + ", unitPricePaid=" + unitPricePaid + '}';
    }
}
