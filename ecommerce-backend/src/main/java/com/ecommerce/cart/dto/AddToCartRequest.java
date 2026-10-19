package com.ecommerce.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AddToCartRequest {

    @NotNull
    @Positive
    private Integer productId;

    @NotNull
    @Positive
    private Integer quantity;
}

