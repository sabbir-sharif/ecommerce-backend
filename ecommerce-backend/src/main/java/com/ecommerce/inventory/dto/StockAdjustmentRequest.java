package com.ecommerce.inventory.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StockAdjustmentRequest {

    @Min(value = 1, message = "Quantity to add must be greater than zero")
    private int quantity;
}