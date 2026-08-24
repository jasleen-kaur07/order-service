package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    boolean existsByCartId(UUID cartId);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(UUID id);

    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
