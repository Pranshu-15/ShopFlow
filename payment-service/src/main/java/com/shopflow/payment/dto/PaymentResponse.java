package com.shopflow.payment.dto;

import com.shopflow.payment.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private Long transactionId;
    private String orderId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String clientSecret;
    private String stripePaymentIntentId;
    private LocalDateTime createdAt;
}
