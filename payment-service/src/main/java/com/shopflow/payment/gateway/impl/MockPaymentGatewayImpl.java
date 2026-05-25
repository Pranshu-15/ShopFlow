package com.shopflow.payment.gateway.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.payment.gateway.StripePaymentGateway;
import com.shopflow.payment.gateway.dto.PaymentIntentResult;
import com.shopflow.payment.gateway.dto.WebhookEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "payment.gateway.mock", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class MockPaymentGatewayImpl implements StripePaymentGateway {

    private final ObjectMapper objectMapper;

    @Override
    public PaymentIntentResult createPaymentIntent(
            String orderId, BigDecimal amount, String currency, String idempotencyKey) {
        String id = "pi_mock_" + UUID.randomUUID().toString().replace("-", "");
        log.info("Mock: created payment intent {} for order {} amount {}{}", id, orderId, amount, currency);
        return PaymentIntentResult.builder()
                .paymentIntentId(id)
                .clientSecret(id + "_secret_" + UUID.randomUUID().toString().replace("-", ""))
                .status("requires_payment_method")
                .build();
    }

    @Override
    public WebhookEventDto parseWebhookEvent(String payload, String sigHeader) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String type = node.path("type").asText();
            String intentId = node.path("data").path("object").path("id").asText();
            String failMsg  = node.path("data").path("object").path("last_payment_error").path("message").asText(null);
            return WebhookEventDto.builder()
                    .type(type)
                    .paymentIntentId(intentId)
                    .failureMessage(failMsg)
                    .build();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook payload");
        }
    }
}
