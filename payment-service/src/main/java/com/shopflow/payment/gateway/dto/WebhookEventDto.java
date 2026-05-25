package com.shopflow.payment.gateway.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEventDto {
    private String type;
    private String paymentIntentId;
    private String failureMessage;
}
