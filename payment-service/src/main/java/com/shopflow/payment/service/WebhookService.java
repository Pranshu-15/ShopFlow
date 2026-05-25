package com.shopflow.payment.service;

import com.shopflow.payment.enums.PaymentStatus;
import com.shopflow.payment.gateway.StripePaymentGateway;
import com.shopflow.payment.gateway.dto.WebhookEventDto;
import com.shopflow.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final StripePaymentGateway paymentGateway;
    private final PaymentTransactionRepository transactionRepository;

    @Transactional
    public void processWebhookEvent(String payload, String sigHeader) {
        WebhookEventDto event = paymentGateway.parseWebhookEvent(payload, sigHeader);

        switch (event.getType()) {
            case "payment_intent.succeeded" ->
                updateStatus(event.getPaymentIntentId(), PaymentStatus.SUCCEEDED, null);
            case "payment_intent.payment_failed" ->
                updateStatus(event.getPaymentIntentId(), PaymentStatus.FAILED, event.getFailureMessage());
            default ->
                log.debug("Unhandled webhook event type: {}", event.getType());
        }
    }

    private void updateStatus(String paymentIntentId, PaymentStatus status, String failureReason) {
        transactionRepository.findByStripePaymentIntentId(paymentIntentId)
                .ifPresentOrElse(txn -> {
                    txn.setStatus(status);
                    txn.setFailureReason(failureReason);
                    transactionRepository.save(txn);
                    log.info("Updated payment {} to status {}", paymentIntentId, status);
                }, () -> log.warn("Received webhook for unknown payment intent: {}", paymentIntentId));
    }
}
