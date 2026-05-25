package com.shopflow.payment.gateway.impl;

import com.shopflow.payment.gateway.StripePaymentGateway;
import com.shopflow.payment.gateway.dto.PaymentIntentResult;
import com.shopflow.payment.gateway.dto.WebhookEventDto;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
@ConditionalOnProperty(name = "payment.gateway.mock", havingValue = "false", matchIfMissing = true)
@Slf4j
public class StripePaymentGatewayImpl implements StripePaymentGateway {

    @Value("${stripe.api-key}")
    private String apiKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @PostConstruct
    void init() {
        Stripe.apiKey = apiKey;
    }

    @Override
    public PaymentIntentResult createPaymentIntent(
            String orderId, BigDecimal amount, String currency, String idempotencyKey) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amount.multiply(BigDecimal.valueOf(100)).longValue())
                    .setCurrency(currency.toLowerCase())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build())
                    .putMetadata("orderId", orderId)
                    .build();

            RequestOptions options = RequestOptions.builder()
                    .setApiKey(apiKey)
                    .setIdempotencyKey(idempotencyKey)
                    .build();

            PaymentIntent intent = PaymentIntent.create(params, options);

            return PaymentIntentResult.builder()
                    .paymentIntentId(intent.getId())
                    .clientSecret(intent.getClientSecret())
                    .status(intent.getStatus())
                    .build();

        } catch (StripeException e) {
            log.error("Stripe API error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment gateway error: " + e.getMessage());
        }
    }

    @Override
    public WebhookEventDto parseWebhookEvent(String payload, String sigHeader) {
        try {
            Event event = com.stripe.net.Webhook.constructEvent(payload, sigHeader, webhookSecret);

            PaymentIntent paymentIntent = event.getDataObjectDeserializer()
                    .getObject()
                    .filter(obj -> obj instanceof PaymentIntent)
                    .map(obj -> (PaymentIntent) obj)
                    .orElse(null);

            String failureMessage = paymentIntent != null && paymentIntent.getLastPaymentError() != null
                    ? paymentIntent.getLastPaymentError().getMessage()
                    : null;

            return WebhookEventDto.builder()
                    .type(event.getType())
                    .paymentIntentId(paymentIntent != null ? paymentIntent.getId() : null)
                    .failureMessage(failureMessage)
                    .build();

        } catch (SignatureVerificationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe signature");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook processing error");
        }
    }
}
