package com.shopflow.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

    private String type; // "ORDER_CREATED" or "ORDER_CONFIRMED"
    private String sagaId;
    private String orderId;
    private String userId;
    private BigDecimal amount;
    private String currency;
}
