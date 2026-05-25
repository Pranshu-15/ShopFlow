package com.shopflow.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.order.PostgresKafkaTestBase;
import com.shopflow.order.dto.OrderItemRequest;
import com.shopflow.order.dto.OrderRequest;
import com.shopflow.order.dto.OrderResponse;
import com.shopflow.order.entity.Order;
import com.shopflow.order.enums.OrderStatus;
import com.shopflow.order.repository.OrderRepository;
import com.shopflow.order.repository.SagaLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SuppressWarnings("null")
class OrderControllerIT extends PostgresKafkaTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SagaLogRepository sagaLogRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String USER_ID  = "test-user-123";
    private static final String BASE_URL = "/api/v1/orders";

    @BeforeEach
    void clearDatabase() {
        orderRepository.deleteAll();
        sagaLogRepository.deleteAll();
    }

    private OrderRequest buildRequest() {
        return OrderRequest.builder()
                .currency("USD")
                .items(List.of(OrderItemRequest.builder()
                        .productId("prod-001")
                        .productName("Blue Widget")
                        .unitPrice(new BigDecimal("49.99"))
                        .quantity(1)
                        .build()))
                .build();
    }

    @Test
    void createOrder_withValidJwt_returnsCreated() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.orderId").isNotEmpty())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void createOrder_withNoAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createOrder_withMissingFields_returnsBadRequest() throws Exception {
        String invalidBody = objectMapper.writeValueAsString(
                OrderRequest.builder().currency("USD").build());

        mockMvc.perform(post(BASE_URL)
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void getMyOrders_returnsOnlyUserOrders() throws Exception {
        // Create two orders for different users
        mockMvc.perform(post(BASE_URL)
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated());

        mockMvc.perform(post(BASE_URL)
                        .with(jwt().jwt(j -> j.subject("other-user-456")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated());

        mockMvc.perform(get(BASE_URL)
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getOrderById_whenOwner_returnsOk() throws Exception {
        String body = mockMvc.perform(post(BASE_URL)
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        OrderResponse response = objectMapper.readValue(body, OrderResponse.class);

        mockMvc.perform(get(BASE_URL + "/" + response.getId())
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getOrderById_whenDifferentUser_returnsForbidden() throws Exception {
        String body = mockMvc.perform(post(BASE_URL)
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        OrderResponse response = objectMapper.readValue(body, OrderResponse.class);

        mockMvc.perform(get(BASE_URL + "/" + response.getId())
                        .with(jwt().jwt(j -> j.subject("other-user-456"))))
                .andExpect(status().isForbidden());
    }

    // Task 8.6 — Saga flow validation: happy path
    @Test
    void sagaFlow_inventoryReservedAndPaymentSucceeded_orderConfirmed() throws Exception {
        String body = mockMvc.perform(post(BASE_URL)
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        OrderResponse created = objectMapper.readValue(body, OrderResponse.class);
        String orderId = created.getOrderId();

        Order savedOrder = orderRepository.findByOrderId(orderId).orElseThrow();
        String sagaId = savedOrder.getSagaId();

        // Simulate inventory service confirming stock reservation
        String inventoryResult = """
                {"type":"INVENTORY_RESULT","sagaId":"%s","orderId":"%s","status":"RESERVED"}
                """.formatted(sagaId, orderId);
        kafkaTemplate.send("inventory-events", orderId, inventoryResult);

        // Simulate payment service confirming payment
        String paymentResult = """
                {"type":"PAYMENT_RESULT","sagaId":"%s","orderId":"%s","status":"SUCCEEDED"}
                """.formatted(sagaId, orderId);
        kafkaTemplate.send("payment-events", orderId, paymentResult);

        await().atMost(15, TimeUnit.SECONDS).until(() -> {
            Order order = orderRepository.findByOrderId(orderId).orElseThrow();
            return order.getStatus() == OrderStatus.CONFIRMED;
        });

        assertThat(sagaLogRepository.findBySagaId(sagaId)).hasSizeGreaterThanOrEqualTo(3);
    }

    // Task 8.6 — Saga compensation: inventory failure
    @Test
    void sagaFlow_inventoryFailed_orderMarkedFailed() throws Exception {
        String body = mockMvc.perform(post(BASE_URL)
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        OrderResponse created = objectMapper.readValue(body, OrderResponse.class);
        String orderId = created.getOrderId();

        Order savedOrder = orderRepository.findByOrderId(orderId).orElseThrow();
        String sagaId = savedOrder.getSagaId();

        // Simulate inventory service reporting out-of-stock
        String inventoryFailed = """
                {"type":"INVENTORY_RESULT","sagaId":"%s","orderId":"%s","status":"FAILED","reason":"Out of stock"}
                """.formatted(sagaId, orderId);
        kafkaTemplate.send("inventory-events", orderId, inventoryFailed);

        await().atMost(15, TimeUnit.SECONDS).until(() -> {
            Order order = orderRepository.findByOrderId(orderId).orElseThrow();
            return order.getStatus() == OrderStatus.FAILED;
        });
    }

    // Task 8.6 — Saga compensation: payment failure triggers inventory rollback
    @Test
    void sagaFlow_paymentFailed_orderFailedAndInventoryRollbackPublished() throws Exception {
        String body = mockMvc.perform(post(BASE_URL)
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        OrderResponse created = objectMapper.readValue(body, OrderResponse.class);
        String orderId = created.getOrderId();

        Order savedOrder = orderRepository.findByOrderId(orderId).orElseThrow();
        String sagaId = savedOrder.getSagaId();

        // Reserve inventory first
        String inventoryResult = """
                {"type":"INVENTORY_RESULT","sagaId":"%s","orderId":"%s","status":"RESERVED"}
                """.formatted(sagaId, orderId);
        kafkaTemplate.send("inventory-events", orderId, inventoryResult);

        // Then payment fails
        String paymentFailed = """
                {"type":"PAYMENT_RESULT","sagaId":"%s","orderId":"%s","status":"FAILED","reason":"Card declined"}
                """.formatted(sagaId, orderId);
        kafkaTemplate.send("payment-events", orderId, paymentFailed);

        await().atMost(15, TimeUnit.SECONDS).until(() -> {
            Order order = orderRepository.findByOrderId(orderId).orElseThrow();
            return order.getStatus() == OrderStatus.FAILED;
        });

        // Verify compensation step was logged
        await().atMost(5, TimeUnit.SECONDS).until(() ->
                sagaLogRepository.findBySagaId(sagaId).stream()
                        .anyMatch(log -> "INVENTORY_ROLLBACK_REQUESTED".equals(log.getStep()))
        );
    }
}
