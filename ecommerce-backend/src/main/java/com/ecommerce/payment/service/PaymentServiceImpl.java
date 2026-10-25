package com.ecommerce.payment.service;

import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.mapper.PaymentMapper;
import com.ecommerce.payment.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public PaymentResponse processPayment(
            int orderId,
            String idempotencyKey,
            PaymentRequest request) {

        if (!StringUtils.hasText(idempotencyKey)
                || idempotencyKey.length() > 100) {
            throw new ConflictException(
                    "A valid Idempotency-Key header is required");
        }

        if (request == null || request.getSimulatedSuccess() == null) {
            throw new ConflictException(
                    "simulatedSuccess must be provided");
        }

        User user = getAuthenticatedUser();

        // Lock the customer's order to serialize payment attempts
        // against that same order.
        Order order = orderRepository
                .findByIdAndUserIdForUpdate(orderId, user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found"));

        // A repeated key must refer to the same order and same request.
        var existingPayment =
                paymentRepository.findByIdempotencyKey(idempotencyKey);

        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();

            boolean sameOrder =
                    payment.getOrder().getId() == order.getId();

            boolean sameRequest =
                    payment.isSimulatedSuccess()
                            == request.getSimulatedSuccess();

            if (!sameOrder || !sameRequest) {
                throw new ConflictException(
                        "Idempotency key was already used for a different request");
            }

            return paymentMapper.toResponse(payment);
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException(
                    "Only pending orders can be paid");
        }

        if (paymentRepository.existsByOrderIdAndStatus(
                order.getId(), PaymentStatus.SUCCEEDED)) {
            throw new ConflictException(
                    "This order already has a successful payment");
        }

        PaymentStatus paymentStatus =
                Boolean.TRUE.equals(request.getSimulatedSuccess())
                        ? PaymentStatus.SUCCEEDED
                        : PaymentStatus.FAILED;

        Payment payment = Payment.builder()
                .order(order)
                .idempotencyKey(idempotencyKey)
                .amount(order.getTotalAmount())
                .status(paymentStatus)
                .simulatedSuccess(request.getSimulatedSuccess())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        return paymentMapper.toResponse(savedPayment);
    }

    private User getAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new ConflictException("User is not authenticated");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
    }
}