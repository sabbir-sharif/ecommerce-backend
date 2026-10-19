package com.ecommerce.cart.controller;

import com.ecommerce.cart.dto.AddToCartRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.dto.UpdateCartItemRequest;
import com.ecommerce.cart.service.CartService;
import com.ecommerce.common.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // 1. Get the authenticated user's cart
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getMyCart() {

        CartResponse cartResponse = cartService.getMyCart();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Cart retrieved successfully",
                        cartResponse
                )
        );
    }

    // 2. Add a product to the cart
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @Valid @RequestBody AddToCartRequest request) {

        CartResponse cartResponse = cartService.addItem(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Product added to cart successfully",
                        cartResponse
                )
        );
    }

    // 3. Update the quantity of a cart item
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @PathVariable int cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request) {

        CartResponse cartResponse =
                cartService.updateItem(cartItemId, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Cart item quantity updated successfully",
                        cartResponse
                )
        );
    }

    // 4. Remove a product from the cart
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @PathVariable int cartItemId) {

        CartResponse cartResponse =
                cartService.removeItem(cartItemId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Product removed from cart successfully",
                        cartResponse
                )
        );
    }

    // 5. Remove all products from the cart
    @DeleteMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> clearCart() {

        CartResponse cartResponse = cartService.clearCart();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Cart cleared successfully",
                        cartResponse
                )
        );
    }
}
