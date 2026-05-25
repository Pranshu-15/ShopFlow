package com.shopflow.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResultEvent {

    private String type; // "INVENTORY_RESULT"
    private String sagaId;
    private String orderId;
    private String status; // "RESERVED" or "FAILED"
    private String reason;
}
