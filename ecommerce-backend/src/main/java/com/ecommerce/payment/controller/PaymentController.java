package com.ecommerce.payment.controller;


import com.ecommerce.common.response.ApiResponse;
import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @PathVariable int orderId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {

        PaymentResponse response = paymentService.processPayment(
                orderId, idempotencyKey, request);

        String message = "SUCCEEDED".equals(response.getStatus())
                ? "Payment successful"
                : "Payment failed";

        return ResponseEntity.ok(
                ApiResponse.success(message, response));
    }
}
