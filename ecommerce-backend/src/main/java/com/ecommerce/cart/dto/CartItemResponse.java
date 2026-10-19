package com.ecommerce.cart.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CartItemResponse {

    private int cartItemId;

    private int productId;

    private String productName;

    private String sku;

    private BigDecimal unitPrice;

    private int quantity;

    private BigDecimal subtotal;
}

