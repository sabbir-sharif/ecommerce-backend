package com.ecommerce.payment.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private int paymentId;
    private int orderId;
    private String orderNumber;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
}
