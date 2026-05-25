package com.shopflow.notification.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.notification.dto.EmailRequest;
import com.shopflow.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topics.order-events}", groupId = "notification-order-group")
    public void consume(String payload) {
        try {
            Map<String, Object> event = objectMapper.readValue(payload, new TypeReference<>() {});
            String type = (String) event.get("type");

            if ("ORDER_CREATED".equals(type)) {
                handleOrderCreated(event);
            } else if ("ORDER_CONFIRMED".equals(type)) {
                handleOrderConfirmed(event);
            }
        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process order event", e);
        }
    }

    private void handleOrderCreated(Map<String, Object> event) {
        String orderId = (String) event.get("orderId");
        String userId  = (String) event.get("userId");

        emailService.send(EmailRequest.builder()
                .toEmail(resolveEmail(userId))
                .toName(userId)
                .subject("Order Received — " + orderId)
                .templateName("orderConfirmation")
                .variables(Map.of(
                        "eventType", "ORDER_RECEIVED",
                        "orderId", orderId,
                        "amount", String.valueOf(event.get("amount")),
                        "currency", String.valueOf(event.get("currency"))
                ))
                .build());
    }

    private void handleOrderConfirmed(Map<String, Object> event) {
        String orderId = (String) event.get("orderId");
        String userId  = (String) event.get("userId");

        emailService.send(EmailRequest.builder()
                .toEmail(resolveEmail(userId))
                .toName(userId)
                .subject("Order Confirmed — " + orderId)
                .templateName("orderConfirmation")
                .variables(Map.of(
                        "eventType", "ORDER_CONFIRMED",
                        "orderId", orderId,
                        "amount", String.valueOf(event.get("amount")),
                        "currency", String.valueOf(event.get("currency"))
                ))
                .build());
    }

    // In production this would call the user-service to look up the actual email address.
    private String resolveEmail(String userId) {
        return userId + "@shopflow.example.com";
    }
}
