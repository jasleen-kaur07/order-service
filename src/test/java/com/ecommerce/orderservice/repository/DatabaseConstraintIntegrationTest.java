package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Tag("integration")
class DatabaseConstraintIntegrationTest {

    private static final UUID DEFAULT_USER =
            UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    private static Order newOrder(UUID cartId, UUID userId) {

        Order order = new Order(
                cartId,
                userId,
                "jasleen@gmail.com",
                "123 Main Street",
                "Apt 4B",
                "Noida",
                "Uttar Pradesh",
                "India",
                "201301",
                new BigDecimal("100.00")
        );

        order.addItem(
                new OrderItem(
                        UUID.randomUUID(),
                        "product-101",
                        "variant-101",
                        "merchant-201",
                        "Widget",
                        "https://example.com/widget.jpg",
                        2,
                        new BigDecimal("50.00"),
                        new BigDecimal("100.00")
                )
        );

        return order;
    }

    @Test
    @DisplayName("saving an order cascades to its items and they can be read back")
    void savesAggregate() {

        Order saved =
                orderRepository.saveAndFlush(
                        newOrder(
                                UUID.randomUUID(),
                                DEFAULT_USER
                        )
                );

        entityManager.clear();

        Order reloaded =
                orderRepository
                        .findWithItemsById(saved.getId())
                        .orElseThrow();

        assertThat(reloaded.getItems())
                .hasSize(1);

        assertThat(
                reloaded.getItems()
                        .get(0)
                        .getProductNameSnapshot()
        )
                .isEqualTo("Widget");

        assertThat(
                reloaded.getItems()
                        .get(0)
                        .getProductId()
        )
                .isEqualTo("product-101");

        assertThat(
                reloaded.getItems()
                        .get(0)
                        .getMerchantId()
        )
                .isEqualTo("merchant-201");

        assertThat(reloaded.getCreatedAt())
                .isNotNull();
    }

    @Test
    @DisplayName("the database refuses a second order for the same cart")
    void uniqueCartIdRejectsDuplicate() {

        UUID cartId = UUID.randomUUID();

        orderRepository.saveAndFlush(
                newOrder(
                        cartId,
                        DEFAULT_USER
                )
        );

        assertThatThrownBy(
                () -> orderRepository.saveAndFlush(
                        newOrder(
                                cartId,
                                DEFAULT_USER
                        )
                )
        )
                .isInstanceOf(
                        DataIntegrityViolationException.class
                );
    }

    @Test
    @DisplayName("the CHECK constraint rejects an unknown order status, even from raw SQL")
    void checkConstraintRejectsUnknownStatus() {

        assertThatThrownBy(() -> {

            entityManager.createNativeQuery("""
                    INSERT INTO orders (
                        id,
                        cart_id,
                        user_id,
                        email_snapshot,
                        ship_line1,
                        ship_city,
                        ship_state,
                        ship_country,
                        ship_pincode,
                        status,
                        total_price,
                        created_at,
                        updated_at
                    )
                    VALUES (
                        :id,
                        :cart,
                        :user,
                        'e@x.com',
                        'l1',
                        'c',
                        's',
                        'co',
                        'p',
                        'SHIPPED',
                        100.00,
                        now(),
                        now()
                    )
                    """)
                    .setParameter(
                            "id",
                            UUID.randomUUID()
                    )
                    .setParameter(
                            "cart",
                            UUID.randomUUID()
                    )
                    .setParameter(
                            "user",
                            DEFAULT_USER
                    )
                    .executeUpdate();

            entityManager.flush();

        })
                .hasMessageContaining(
                        "ck_orders_status"
                );
    }

    @Test
    @DisplayName("the CHECK constraint rejects a non-positive line quantity, even from raw SQL")
    void checkConstraintRejectsZeroQuantity() {

        Order order =
                orderRepository.saveAndFlush(
                        newOrder(
                                UUID.randomUUID(),
                                DEFAULT_USER
                        )
                );

        assertThatThrownBy(() -> {

            entityManager.createNativeQuery("""
                    INSERT INTO order_items (
                        id,
                        order_id,
                        product_id,
                        merchant_id,
                        product_name_snapshot,
                        quantity,
                        unit_price_paid,
                        created_at,
                        updated_at
                    )
                    VALUES (
                        :id,
                        :order,
                        :product,
                        :merchant,
                        'Widget',
                        0,
                        10.00,
                        now(),
                        now()
                    )
                    """)
                    .setParameter(
                            "id",
                            UUID.randomUUID()
                    )
                    .setParameter(
                            "order",
                            order.getId()
                    )
                    .setParameter(
                            "product",
                            "product-101"
                    )
                    .setParameter(
                            "merchant",
                            "merchant-201"
                    )
                    .executeUpdate();

            entityManager.flush();

        })
                .hasMessageContaining(
                        "ck_order_items_quantity"
                );
    }

    @Test
    @DisplayName("deleting an order cascades to its items, leaving no orphans")
    void deletingOrderCascadesToItems() {

        Order order =
                orderRepository.saveAndFlush(
                        newOrder(
                                UUID.randomUUID(),
                                DEFAULT_USER
                        )
                );

        UUID orderId = order.getId();

        entityManager.createNativeQuery(
                        "DELETE FROM orders WHERE id = :id"
                )
                .setParameter(
                        "id",
                        orderId
                )
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        Number remainingItems =
                (Number) entityManager
                        .createNativeQuery(
                                "SELECT count(*) FROM order_items " +
                                        "WHERE order_id = :id"
                        )
                        .setParameter(
                                "id",
                                orderId
                        )
                        .getSingleResult();

        assertThat(
                remainingItems.longValue()
        )
                .isZero();
    }

    @Test
    @DisplayName("order history returns only the given customer's orders")
    void historyIsScopedToTheUser() {

        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        orderRepository.saveAndFlush(
                newOrder(
                        UUID.randomUUID(),
                        userA
                )
        );

        orderRepository.saveAndFlush(
                newOrder(
                        UUID.randomUUID(),
                        userA
                )
        );

        orderRepository.saveAndFlush(
                newOrder(
                        UUID.randomUUID(),
                        userB
                )
        );

        assertThat(
                orderRepository
                        .findByUserIdOrderByCreatedAtDesc(userA)
        )
                .hasSize(2)
                .allMatch(
                        order ->
                                order.getUserId()
                                        .equals(userA)
                );
    }
}