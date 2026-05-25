package com.shopflow.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReserveEvent {

    private String type; // "INVENTORY_RESERVE_REQUEST" or "INVENTORY_ROLLBACK_REQUEST"
    private String sagaId;
    private String orderId;
    private List<InventoryItemEvent> items;
}
