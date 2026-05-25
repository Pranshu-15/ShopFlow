package com.shopflow.cart.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CartItem {

    private Long productId;
    private String productName;
    private String sku;
    private BigDecimal unitPrice;
    private int quantity;
    private String imageUrl;

    public BigDecimal getSubtotal() {
        return unitPrice != null ? unitPrice.multiply(BigDecimal.valueOf(quantity)) : BigDecimal.ZERO;
    }
}
