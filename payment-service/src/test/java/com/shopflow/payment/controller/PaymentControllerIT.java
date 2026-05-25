package com.shopflow.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.payment.PostgresTestContainerBase;
import com.shopflow.payment.dto.PaymentRequest;
import com.shopflow.payment.entity.PaymentTransaction;
import com.shopflow.payment.enums.PaymentStatus;
import com.shopflow.payment.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SuppressWarnings("null")
class PaymentControllerIT extends PostgresTestContainerBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentTransactionRepository transactionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String USER_ID  = "test-user-123";
    private static final String BASE_URL = "/api/v1/payments";

    @BeforeEach
    void clearDatabase() {
        transactionRepository.deleteAll();
    }

    private PaymentRequest buildRequest() {
        return PaymentRequest.builder()
                .orderId("ORDER-" + UUID.randomUUID().toString().substring(0, 8))
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
    }

    private PaymentTransaction saveTransaction(String userId, PaymentStatus status) {
        return transactionRepository.save(PaymentTransaction.builder()
                .orderId("ORDER-001")
                .userId(userId)
                .amount(new BigDecimal("49.99"))
                .currency("USD")
                .status(status)
                .stripePaymentIntentId("pi_mock_test123")
                .clientSecret("pi_mock_test123_secret")
                .idempotencyKeyValue(UUID.randomUUID().toString())
                .build());
    }

    @Test
    void initiatePayment_withValidJwt_returnsCreated() throws Exception {
        String body = objectMapper.writeValueAsString(buildRequest());

        mockMvc.perform(post(BASE_URL)
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.clientSecret").isNotEmpty());
    }

    @Test
    void initiatePayment_withNoAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void initiatePayment_withMissingFields_returnsBadRequest() throws Exception {
        String invalidBody = objectMapper.writeValueAsString(
                PaymentRequest.builder().orderId("ORDER-X").build());

        mockMvc.perform(post(BASE_URL)
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void initiatePayment_withSameIdempotencyKey_returnsCachedResponse() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        PaymentRequest request = PaymentRequest.builder()
                .orderId("ORDER-IDEM")
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .idempotencyKey(idempotencyKey)
                .build();
        String body = objectMapper.writeValueAsString(request);

        // First call
        mockMvc.perform(post(BASE_URL)
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Second call with same idempotency key — should return cached response
        mockMvc.perform(post(BASE_URL)
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Only one transaction created
        long count = transactionRepository.count();
        assert count == 1;
    }

    @Test
    void getMyPayments_returnsOnlyUserPayments() throws Exception {
        saveTransaction(USER_ID, PaymentStatus.SUCCEEDED);
        saveTransaction("other-user-456", PaymentStatus.PENDING);

        mockMvc.perform(get(BASE_URL)
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getPaymentById_whenOwner_returnsOk() throws Exception {
        PaymentTransaction saved = saveTransaction(USER_ID, PaymentStatus.SUCCEEDED);

        mockMvc.perform(get(BASE_URL + "/" + saved.getId())
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }

    @Test
    void getPaymentById_whenDifferentUser_returnsForbidden() throws Exception {
        PaymentTransaction saved = saveTransaction("other-user-456", PaymentStatus.PENDING);

        mockMvc.perform(get(BASE_URL + "/" + saved.getId())
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isForbidden());
    }

    @Test
    void webhookEndpoint_withMockPayload_returns200() throws Exception {
        saveTransaction(USER_ID, PaymentStatus.PENDING);
        String payload = """
                {
                  "type": "payment_intent.succeeded",
                  "data": {"object": {"id": "pi_mock_test123"}}
                }
                """;

        mockMvc.perform(post(BASE_URL + "/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }
}
