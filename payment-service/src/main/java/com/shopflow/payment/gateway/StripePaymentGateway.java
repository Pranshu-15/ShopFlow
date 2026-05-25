package com.shopflow.payment.gateway;

import com.shopflow.payment.gateway.dto.PaymentIntentResult;
import com.shopflow.payment.gateway.dto.WebhookEventDto;

import java.math.BigDecimal;

public interface StripePaymentGateway {

    PaymentIntentResult createPaymentIntent(
            String orderId,
            BigDecimal amount,
            String currency,
            String idempotencyKey);

    WebhookEventDto parseWebhookEvent(String payload, String sigHeader);
}
