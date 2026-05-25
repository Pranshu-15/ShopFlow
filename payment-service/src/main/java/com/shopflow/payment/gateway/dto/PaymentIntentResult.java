package com.shopflow.payment.gateway.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentIntentResult {
    private String paymentIntentId;
    private String clientSecret;
    private String status;
}
