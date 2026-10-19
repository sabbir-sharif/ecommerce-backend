package com.ecommerce.cart.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CartResponse {

    private int cartId;

    private int userId;

    private List<CartItemResponse> items;

    private int totalItems;

    private BigDecimal totalPrice;
}

