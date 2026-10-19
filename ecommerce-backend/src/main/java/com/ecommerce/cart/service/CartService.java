package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.AddToCartRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.dto.UpdateCartItemRequest;

public interface CartService {

    CartResponse getMyCart();

    CartResponse addItem(AddToCartRequest request);

    CartResponse updateItem(
            int cartItemId,
            UpdateCartItemRequest request
    );

    CartResponse removeItem(int cartItemId);

    CartResponse clearCart();
}
