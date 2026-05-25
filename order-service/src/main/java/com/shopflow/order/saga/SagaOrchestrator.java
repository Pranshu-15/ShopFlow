package com.shopflow.order.saga;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.order.entity.Order;
import com.shopflow.order.entity.SagaLog;
import com.shopflow.order.enums.OrderStatus;
import com.shopflow.order.event.InventoryItemEvent;
import com.shopflow.order.event.InventoryReserveEvent;
import com.shopflow.order.event.OrderCreatedEvent;
import com.shopflow.order.event.PaymentRequestedEvent;
import com.shopflow.order.repository.OrderRepository;
import com.shopflow.order.repository.SagaLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestrator {

    private final OrderRepository orderRepository;
    private final SagaLogRepository sagaLogRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.order-events}")
    private String orderEventsTopic;

    @Value("${kafka.topics.inventory-events}")
    private String inventoryEventsTopic;

    @Value("${kafka.topics.payment-events}")
    private String paymentEventsTopic;

    public void startSaga(Order order) {
        logStep(order.getSagaId(), order.getOrderId(), "INVENTORY_RESERVE_REQUESTED", "PENDING", null);

        InventoryReserveEvent event = InventoryReserveEvent.builder()
                .type("INVENTORY_RESERVE_REQUEST")
                .sagaId(order.getSagaId())
                .orderId(order.getOrderId())
                .items(order.getItems().stream()
                        .map(i -> InventoryItemEvent.builder()
                                .productId(i.getProductId())
                                .quantity(i.getQuantity())
                                .build())
                        .toList())
                .build();

        publish(inventoryEventsTopic, order.getOrderId(), event);

        OrderCreatedEvent createdEvent = OrderCreatedEvent.builder()
                .type("ORDER_CREATED")
                .sagaId(order.getSagaId())
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .build();
        publish(orderEventsTopic, order.getOrderId(), createdEvent);
    }

    // Handles results from inventory service. Ignores reserve/rollback commands this service published.
    @KafkaListener(topics = "${kafka.topics.inventory-events}", groupId = "order-saga-group")
    @Transactional
    public void handleInventoryEvent(String payload) {
        try {
            Map<String, Object> event = objectMapper.readValue(payload, new TypeReference<>() {});
            String type = (String) event.get("type");

            if (!"INVENTORY_RESULT".equals(type)) return;

            String sagaId  = (String) event.get("sagaId");
            String orderId = (String) event.get("orderId");
            String status  = (String) event.get("status");

            Order order = orderRepository.findByOrderId(orderId).orElse(null);
            if (order == null) {
                log.warn("Received inventory result for unknown orderId: {}", orderId);
                return;
            }

            if ("RESERVED".equals(status)) {
                logStep(sagaId, orderId, "INVENTORY_RESERVED", "SUCCESS", payload);
                requestPayment(order);
            } else {
                logStep(sagaId, orderId, "INVENTORY_FAILED", "FAILED", payload);
                order.setStatus(OrderStatus.FAILED);
                orderRepository.save(order);
            }
        } catch (Exception e) {
            log.error("Error processing inventory event: {}", e.getMessage(), e);
        }
    }

    // Handles results from payment service. Ignores payment requests this service published.
    @KafkaListener(topics = "${kafka.topics.payment-events}", groupId = "order-saga-group")
    @Transactional
    public void handlePaymentEvent(String payload) {
        try {
            Map<String, Object> event = objectMapper.readValue(payload, new TypeReference<>() {});
            String type = (String) event.get("type");

            if (!"PAYMENT_RESULT".equals(type)) return;

            String sagaId  = (String) event.get("sagaId");
            String orderId = (String) event.get("orderId");
            String status  = (String) event.get("status");

            Order order = orderRepository.findByOrderId(orderId).orElse(null);
            if (order == null) {
                log.warn("Received payment result for unknown orderId: {}", orderId);
                return;
            }

            if ("SUCCEEDED".equals(status)) {
                logStep(sagaId, orderId, "PAYMENT_SUCCEEDED", "SUCCESS", payload);
                order.setStatus(OrderStatus.CONFIRMED);
                orderRepository.save(order);
                publishOrderConfirmed(order);
            } else {
                logStep(sagaId, orderId, "PAYMENT_FAILED", "FAILED", payload);
                order.setStatus(OrderStatus.FAILED);
                orderRepository.save(order);
                compensateInventory(order, sagaId);
            }
        } catch (Exception e) {
            log.error("Error processing payment event: {}", e.getMessage(), e);
        }
    }

    private void requestPayment(Order order) {
        logStep(order.getSagaId(), order.getOrderId(), "PAYMENT_REQUESTED", "PENDING", null);

        PaymentRequestedEvent event = PaymentRequestedEvent.builder()
                .type("PAYMENT_REQUEST")
                .sagaId(order.getSagaId())
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .build();

        publish(paymentEventsTopic, order.getOrderId(), event);
    }

    // Saga compensation: rollback inventory reservation when payment fails
    private void compensateInventory(Order order, String sagaId) {
        logStep(sagaId, order.getOrderId(), "INVENTORY_ROLLBACK_REQUESTED", "COMPENSATING", null);

        InventoryReserveEvent rollback = InventoryReserveEvent.builder()
                .type("INVENTORY_ROLLBACK_REQUEST")
                .sagaId(sagaId)
                .orderId(order.getOrderId())
                .items(order.getItems().stream()
                        .map(i -> InventoryItemEvent.builder()
                                .productId(i.getProductId())
                                .quantity(i.getQuantity())
                                .build())
                        .toList())
                .build();

        publish(inventoryEventsTopic, order.getOrderId(), rollback);
    }

    private void publishOrderConfirmed(Order order) {
        OrderCreatedEvent confirmed = OrderCreatedEvent.builder()
                .type("ORDER_CONFIRMED")
                .sagaId(order.getSagaId())
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .build();
        publish(orderEventsTopic, order.getOrderId(), confirmed);
    }

    private void publish(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event for topic {}: {}", topic, e.getMessage(), e);
        }
    }

    private void logStep(String sagaId, String orderId, String step, String status, String payload) {
        sagaLogRepository.save(SagaLog.builder()
                .sagaId(sagaId)
                .orderId(orderId)
                .step(step)
                .status(status)
                .payload(payload)
                .build());
    }
}
