package com.ecommerce.payment.repository;

import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Integer> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    boolean existsByOrderIdAndStatus(
            int orderId,
            PaymentStatus status
    );
}
