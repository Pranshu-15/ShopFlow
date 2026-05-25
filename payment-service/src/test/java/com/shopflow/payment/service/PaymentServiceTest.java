package com.shopflow.payment.service;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class PaymentServiceTest {

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Mock
    private StripePaymentGateway paymentGateway;

    @InjectMocks
    private PaymentService paymentService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private static final String USER_ID = "test-user-123";

    private PaymentRequest buildRequest(String idempotencyKey) {
        return PaymentRequest.builder()
                .orderId("ORDER-001")
                .amount(new BigDecimal("49.99"))
                .currency("USD")
                .idempotencyKey(idempotencyKey)
                .build();
    }

    private PaymentTransaction buildSavedTransaction() {
        return PaymentTransaction.builder()
                .id(1L)
                .orderId("ORDER-001")
                .userId(USER_ID)
                .amount(new BigDecimal("49.99"))
                .currency("USD")
                .status(PaymentStatus.PENDING)
                .stripePaymentIntentId("pi_mock_abc123")
                .clientSecret("pi_mock_abc123_secret_xyz")
                .idempotencyKeyValue("idem-key-001")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void processPayment_withNewIdempotencyKey_createsTransactionAndCachesResponse() throws Exception {
        when(idempotencyKeyRepository.findByKeyValueAndExpiresAtAfter(anyString(), any()))
                .thenReturn(Optional.empty());
        when(paymentGateway.createPaymentIntent(anyString(), any(), anyString(), anyString()))
                .thenReturn(PaymentIntentResult.builder()
                        .paymentIntentId("pi_mock_abc123")
                        .clientSecret("pi_mock_abc123_secret_xyz")
                        .status("requires_payment_method")
                        .build());
        when(transactionRepository.save(any(PaymentTransaction.class)))
                .thenReturn(buildSavedTransaction());
        when(idempotencyKeyRepository.save(any(IdempotencyKey.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Inject real ObjectMapper via reflection since @InjectMocks doesn't handle it
        var field = PaymentService.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(paymentService, objectMapper);

        PaymentResponse response = paymentService.processPayment(USER_ID, buildRequest("idem-key-001"));

        assertThat(response.getOrderId()).isEqualTo("ORDER-001");
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.getClientSecret()).isEqualTo("pi_mock_abc123_secret_xyz");
        verify(idempotencyKeyRepository).save(any(IdempotencyKey.class));
    }

    @Test
    void processPayment_withExistingIdempotencyKey_returnsCachedResponse() throws Exception {
        PaymentResponse cached = PaymentResponse.builder()
                .transactionId(1L)
                .orderId("ORDER-001")
                .status(PaymentStatus.PENDING)
                .build();

        IdempotencyKey key = IdempotencyKey.builder()
                .keyValue("idem-key-001")
                .responseBody(objectMapper.writeValueAsString(cached))
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        var field = PaymentService.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(paymentService, objectMapper);

        when(idempotencyKeyRepository.findByKeyValueAndExpiresAtAfter(anyString(), any()))
                .thenReturn(Optional.of(key));

        PaymentResponse response = paymentService.processPayment(USER_ID, buildRequest("idem-key-001"));

        assertThat(response.getTransactionId()).isEqualTo(1L);
        verify(paymentGateway, never()).createPaymentIntent(any(), any(), any(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void getPaymentsByUser_returnsAllTransactions() {
        when(transactionRepository.findByUserId(USER_ID))
                .thenReturn(List.of(buildSavedTransaction()));

        List<PaymentResponse> payments = paymentService.getPaymentsByUser(USER_ID);

        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void getPaymentById_whenFound_returnsResponse() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(buildSavedTransaction()));

        PaymentResponse response = paymentService.getPaymentById(1L, USER_ID);

        assertThat(response.getTransactionId()).isEqualTo(1L);
    }

    @Test
    void getPaymentById_whenNotFound_throwsNotFoundException() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById(99L, USER_ID))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getPaymentById_whenDifferentUser_throwsForbidden() {
        PaymentTransaction txn = buildSavedTransaction();
        txn.setUserId("other-user-456");
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(txn));

        assertThatThrownBy(() -> paymentService.getPaymentById(1L, USER_ID))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
