package com.ecommerce.orderservice.entity;

import com.ecommerce.orderservice.exception.ConflictException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "cart_id", nullable = false, updatable = false)
    private UUID cartId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "email_snapshot", nullable = false, length = 255, updatable = false)
    private String emailSnapshot;

    @Column(name = "first_name_snapshot", length = 100, updatable = false)
    private String firstNameSnapshot;

    @Column(name = "last_name_snapshot", length = 100, updatable = false)
    private String lastNameSnapshot;

    @Column(name = "ship_line1", nullable = false, length = 255, updatable = false)
    private String shipLine1;

    @Column(name = "ship_line2", length = 255, updatable = false)
    private String shipLine2;

    @Column(name = "ship_city", nullable = false, length = 100, updatable = false)
    private String shipCity;

    @Column(name = "ship_state", nullable = false, length = 100, updatable = false)
    private String shipState;

    @Column(name = "ship_country", nullable = false, length = 100, updatable = false)
    private String shipCountry;

    @Column(name = "ship_pincode", nullable = false, length = 20, updatable = false)
    private String shipPincode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal totalPrice;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    public Order(UUID cartId, UUID userId, String emailSnapshot,
                 String firstNameSnapshot, String lastNameSnapshot,
                 String shipLine1, String shipLine2, String shipCity, String shipState,
                 String shipCountry, String shipPincode, BigDecimal totalPrice) {
        this.cartId = cartId;
        this.userId = userId;
        this.emailSnapshot = emailSnapshot;
        this.firstNameSnapshot = firstNameSnapshot;
        this.lastNameSnapshot = lastNameSnapshot;
        this.shipLine1 = shipLine1;
        this.shipLine2 = shipLine2;
        this.shipCity = shipCity;
        this.shipState = shipState;
        this.shipCountry = shipCountry;
        this.shipPincode = shipPincode;
        this.totalPrice = totalPrice;
        this.status = OrderStatus.CREATED;
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        this.items.add(item);
    }

    public void cancel() {
        if (this.status != OrderStatus.CREATED) {
            throw ConflictException.notCancellable(this.id, this.status);
        }
        this.status = OrderStatus.CANCELLED;
    }

    public boolean isCancelled() {
        return this.status == OrderStatus.CANCELLED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCartId() {
        return cartId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmailSnapshot() {
        return emailSnapshot;
    }

    public String getFirstNameSnapshot() {
        return firstNameSnapshot;
    }

    public String getLastNameSnapshot() {
        return lastNameSnapshot;
    }

    public String getShipLine1() {
        return shipLine1;
    }

    public String getShipLine2() {
        return shipLine2;
    }

    public String getShipCity() {
        return shipCity;
    }

    public String getShipState() {
        return shipState;
    }

    public String getShipCountry() {
        return shipCountry;
    }

    public String getShipPincode() {
        return shipPincode;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Order order)) {
            return false;
        }
        return id != null && Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Order{id=" + id + ", userId=" + userId + ", status=" + status
                + ", totalPrice=" + totalPrice + '}';
    }
}