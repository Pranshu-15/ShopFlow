package com.shopflow.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResultEvent {

    private String type; // "PAYMENT_RESULT"
    private String sagaId;
    private String orderId;
    private String status; // "SUCCEEDED" or "FAILED"
    private String reason;
    private String paymentIntentId;
}
