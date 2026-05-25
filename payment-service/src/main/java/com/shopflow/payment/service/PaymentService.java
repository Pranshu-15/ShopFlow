package com.shopflow.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.payment.dto.PaymentRequest;
import com.shopflow.payment.dto.PaymentResponse;
import com.shopflow.payment.entity.IdempotencyKey;
import com.shopflow.payment.entity.PaymentTransaction;
import com.shopflow.payment.enums.PaymentStatus;
import com.shopflow.payment.gateway.StripePaymentGateway;
import com.shopflow.payment.gateway.dto.PaymentIntentResult;
import com.shopflow.payment.repository.IdempotencyKeyRepository;
import com.shopflow.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentTransactionRepository transactionRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final StripePaymentGateway paymentGateway;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentResponse processPayment(String userId, PaymentRequest request) {
        // Check idempotency cache
        var cachedKey = idempotencyKeyRepository
                .findByKeyValueAndExpiresAtAfter(request.getIdempotencyKey(), LocalDateTime.now());

        if (cachedKey.isPresent()) {
            log.debug("Returning cached response for idempotency key: {}", request.getIdempotencyKey());
            return deserializeResponse(cachedKey.get().getResponseBody());
        }

        // Create payment intent
        PaymentIntentResult intentResult = paymentGateway.createPaymentIntent(
                request.getOrderId(), request.getAmount(), request.getCurrency(), request.getIdempotencyKey());

        // Persist transaction
        PaymentTransaction transaction = PaymentTransaction.builder()
                .orderId(request.getOrderId())
                .userId(userId)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(PaymentStatus.PENDING)
                .stripePaymentIntentId(intentResult.getPaymentIntentId())
                .clientSecret(intentResult.getClientSecret())
                .idempotencyKeyValue(request.getIdempotencyKey())
                .build();

        transaction = transactionRepository.save(transaction);
        PaymentResponse response = mapToResponse(transaction);

        // Cache idempotency key for 24 hours
        idempotencyKeyRepository.save(IdempotencyKey.builder()
                .keyValue(request.getIdempotencyKey())
                .responseBody(serializeResponse(response))
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build());

        return response;
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByUser(String userId) {
        return transactionRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id, String userId) {
        PaymentTransaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (!transaction.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return mapToResponse(transaction);
    }

    PaymentResponse mapToResponse(PaymentTransaction transaction) {
        return PaymentResponse.builder()
                .transactionId(transaction.getId())
                .orderId(transaction.getOrderId())
                .userId(transaction.getUserId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus())
                .clientSecret(transaction.getClientSecret())
                .stripePaymentIntentId(transaction.getStripePaymentIntentId())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private String serializeResponse(PaymentResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Serialization error");
        }
    }

    private PaymentResponse deserializeResponse(String json) {
        try {
            return objectMapper.readValue(json, PaymentResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Deserialization error");
        }
    }
}
