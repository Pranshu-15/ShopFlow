package com.shopflow.cart.dto;

import jakarta.validation.constraints.Min;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateQuantityRequest {

    @Min(value = 0, message = "Quantity cannot be negative")
    private int quantity;
}
