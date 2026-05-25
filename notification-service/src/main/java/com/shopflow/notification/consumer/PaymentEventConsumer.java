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
public class PaymentEventConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topics.payment-events}", groupId = "notification-payment-group")
    public void consume(String payload) {
        try {
            Map<String, Object> event = objectMapper.readValue(payload, new TypeReference<>() {});
            String type   = (String) event.get("type");
            String status = (String) event.get("status");

            if (!"PAYMENT_RESULT".equals(type)) return;

            if ("SUCCEEDED".equals(status)) {
                handlePaymentSucceeded(event);
            } else if ("FAILED".equals(status)) {
                handlePaymentFailed(event);
            }
        } catch (Exception e) {
            log.error("Error processing payment event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process payment event", e);
        }
    }

    private void handlePaymentSucceeded(Map<String, Object> event) {
        String orderId = (String) event.get("orderId");
        String userId  = (String) event.get("userId");

        emailService.send(EmailRequest.builder()
                .toEmail(resolveEmail(userId))
                .toName(userId)
                .subject("Payment Receipt — " + orderId)
                .templateName("paymentReceipt")
                .variables(Map.of(
                        "orderId", orderId,
                        "status", "SUCCEEDED",
                        "amount", String.valueOf(event.getOrDefault("amount", "")),
                        "currency", String.valueOf(event.getOrDefault("currency", ""))
                ))
                .build());
    }

    private void handlePaymentFailed(Map<String, Object> event) {
        String orderId = (String) event.get("orderId");
        String userId  = (String) event.get("userId");

        emailService.send(EmailRequest.builder()
                .toEmail(resolveEmail(userId))
                .toName(userId)
                .subject("Payment Failed — " + orderId)
                .templateName("paymentReceipt")
                .variables(Map.of(
                        "orderId", orderId,
                        "status", "FAILED",
                        "reason", String.valueOf(event.getOrDefault("reason", "Unknown reason"))
                ))
                .build());
    }

    private String resolveEmail(String userId) {
        return userId + "@shopflow.example.com";
    }
}
