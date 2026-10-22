package com.ecommerce.order.controller;

import com.ecommerce.common.response.ApiResponse;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.service.OrderService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // Place an order from the authenticated user's cart
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder() {

        OrderResponse response = orderService.placeOrder();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Order placed successfully",
                        response
                ));
    }

    // Get all orders belonging to the authenticated user
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders() {

        List<OrderResponse> response = orderService.getMyOrders();

        return ResponseEntity.ok(ApiResponse.success(
                "Orders retrieved successfully",
                response
        ));
    }

    // Get a specific order belonging to the authenticated user
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getMyOrderById(
            @PathVariable int orderId) {

        OrderResponse response = orderService.getMyOrderById(orderId);

        return ResponseEntity.ok(ApiResponse.success(
                "Order retrieved successfully",
                response
        ));
    }

    // Cancel order
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelMyOrder(
            @PathVariable int orderId) {

        OrderResponse response = orderService.cancelMyOrder(orderId);

        return ResponseEntity.ok(ApiResponse.success(
                "Order cancelled successfully",
                response
        ));
    }
}
