package com.ecommerce.orderservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8085}")
    private int serverPort;

    @Bean
    public OpenAPI orderServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Service API")
                        .version("v1")
                        .description("""
                                Orders, order items, order status, order history and order events for the
                                Ecommerce Platform.

                                ### What this service owns
                                * order creation (called by Cart Service at checkout)
                                * order retrieval, order history and order status
                                * order cancellation
                                * the `orders` and `order_items` tables
                                * publishing **OrderCreated** and **OrderCancelled** Kafka events

                                ### What it deliberately does not own
                                Authentication, passwords, JWTs and user accounts belong to **Auth Service**.
                                User email and delivery addresses belong to **User Service** (Order Service
                                reads them at checkout and snapshots them). Carts belong to **Cart Service**;
                                the product catalogue to **Product Service**; stock, inventory and pricing to
                                **Merchant Service**; emails to **Notification Service**.

                                No other service reads `order_service_db` directly, and this service reads no
                                other service's database.

                                ### Security
                                This service performs **no** authentication or authorization. The API Gateway
                                validates the JWT and Auth Service owns identity; requests that reach Order
                                Service are trusted to have already passed that flow.""")
                        .contact(new Contact().name("Order Service owner")))
                .servers(List.of(new Server()
                        .url("http://localhost:" + serverPort)
                        .description("Local development")));
    }
}
