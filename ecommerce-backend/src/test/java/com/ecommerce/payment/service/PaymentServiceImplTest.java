
package com.ecommerce.payment.service;

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
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User user;
    private Order order;
    private PaymentResponse paymentResponse;

    // --------------------------------------------------
    // TEST SETUP
    // --------------------------------------------------

    @BeforeEach
    void setUp() {

        user = new User();
        user.setId(10);
        user.setName("Test User");
        user.setEmail("test@example.com");

        order = new Order();
        order.setId(25);
        order.setOrderNumber("ORD-TEST-001");
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("125.50"));

        paymentResponse = PaymentResponse.builder()
                .paymentId(100)
                .orderId(25)
                .orderNumber("ORD-TEST-001")
                .amount(new BigDecimal("125.50"))
                .status("SUCCEEDED")
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --------------------------------------------------
    // HELPER METHODS
    // --------------------------------------------------

    private void authenticateUser() {

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "test@example.com",
                        null,
                        List.of()
                )
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));
    }

    private void setAuthenticationOnly() {

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "test@example.com",
                        null,
                        List.of()
                )
        );
    }

    private PaymentRequest paymentRequest(Boolean simulatedSuccess) {

        return PaymentRequest.builder()
                .simulatedSuccess(simulatedSuccess)
                .build();
    }

    private void prepareOrderLookup() {

        when(orderRepository.findByIdAndUserIdForUpdate(25, 10))
                .thenReturn(Optional.of(order));
    }

    // --------------------------------------------------
    // TEST 1: SUCCESSFUL PAYMENT
    // --------------------------------------------------

    @Test
    void processPayment_shouldProcessSuccessfulPayment() {

        authenticateUser();
        prepareOrderLookup();

        when(paymentRepository.findByIdempotencyKey("payment-key-001"))
                .thenReturn(Optional.empty());

        when(paymentRepository.existsByOrderIdAndStatus(
                25, PaymentStatus.SUCCEEDED))
                .thenReturn(false);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(paymentMapper.toResponse(any(Payment.class)))
                .thenReturn(paymentResponse);

        PaymentResponse result = paymentService.processPayment(
                25,
                "payment-key-001",
                paymentRequest(true)
        );

        assertNotNull(result);
        assertEquals(100, result.getPaymentId());
        assertEquals(25, result.getOrderId());
        assertEquals("SUCCEEDED", result.getStatus());
        assertEquals(
                new BigDecimal("125.50"),
                result.getAmount()
        );

        ArgumentCaptor<Payment> paymentCaptor =
                ArgumentCaptor.forClass(Payment.class);

        verify(paymentRepository).save(paymentCaptor.capture());

        Payment savedPayment = paymentCaptor.getValue();

        assertEquals(order, savedPayment.getOrder());
        assertEquals(
                "payment-key-001",
                savedPayment.getIdempotencyKey()
        );
        assertEquals(
                new BigDecimal("125.50"),
                savedPayment.getAmount()
        );
        assertEquals(
                PaymentStatus.SUCCEEDED,
                savedPayment.getStatus()
        );
        assertTrue(savedPayment.isSimulatedSuccess());

        verify(paymentMapper).toResponse(savedPayment);
    }

    // --------------------------------------------------
    // TEST 2: FAILED PAYMENT
    // --------------------------------------------------

    @Test
    void processPayment_shouldSaveFailedPayment() {

        authenticateUser();
        prepareOrderLookup();

        when(paymentRepository.findByIdempotencyKey("payment-key-002"))
                .thenReturn(Optional.empty());

        when(paymentRepository.existsByOrderIdAndStatus(
                25, PaymentStatus.SUCCEEDED))
                .thenReturn(false);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse failedResponse = PaymentResponse.builder()
                .paymentId(101)
                .orderId(25)
                .orderNumber("ORD-TEST-001")
                .amount(new BigDecimal("125.50"))
                .status("FAILED")
                .build();

        when(paymentMapper.toResponse(any(Payment.class)))
                .thenReturn(failedResponse);

        PaymentResponse result = paymentService.processPayment(
                25,
                "payment-key-002",
                paymentRequest(false)
        );

        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());

        ArgumentCaptor<Payment> paymentCaptor =
                ArgumentCaptor.forClass(Payment.class);

        verify(paymentRepository).save(paymentCaptor.capture());

        Payment savedPayment = paymentCaptor.getValue();

        assertEquals(
                PaymentStatus.FAILED,
                savedPayment.getStatus()
        );

        assertFalse(savedPayment.isSimulatedSuccess());
    }

    // --------------------------------------------------
    // TEST 3: BLANK IDEMPOTENCY KEY
    // --------------------------------------------------

    @Test
    void processPayment_shouldRejectBlankIdempotencyKey() {

        assertThrows(
                ConflictException.class,
                () -> paymentService.processPayment(
                        25,
                        "   ",
                        paymentRequest(true)
                )
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(paymentMapper);
    }

    // --------------------------------------------------
    // TEST 4: IDEMPOTENCY KEY TOO LONG
    // --------------------------------------------------

    @Test
    void processPayment_shouldRejectIdempotencyKeyLongerThan100Characters() {

        String longKey = "a".repeat(101);

        assertThrows(
                ConflictException.class,
                () -> paymentService.processPayment(
                        25,
                        longKey,
                        paymentRequest(true)
                )
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(paymentMapper);
    }

    // --------------------------------------------------
    // TEST 5: NULL PAYMENT REQUEST
    // --------------------------------------------------

    @Test
    void processPayment_shouldRejectNullRequest() {

        assertThrows(
                ConflictException.class,
                () -> paymentService.processPayment(
                        25,
                        "payment-key-005",
                        null
                )
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(paymentMapper);
    }

    // --------------------------------------------------
    // TEST 6: NULL SIMULATED SUCCESS
    // --------------------------------------------------

    @Test
    void processPayment_shouldRejectNullSimulatedSuccess() {

        assertThrows(
                ConflictException.class,
                () -> paymentService.processPayment(
                        25,
                        "payment-key-006",
                        paymentRequest(null)
                )
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(paymentMapper);
    }

    // --------------------------------------------------
    // TEST 7: UNAUTHENTICATED USER
    // --------------------------------------------------

    @Test
    void processPayment_shouldRejectUnauthenticatedUser() {

        SecurityContextHolder.clearContext();

        assertThrows(
                ConflictException.class,
                () -> paymentService.processPayment(
                        25,
                        "payment-key-007",
                        paymentRequest(true)
                )
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(paymentMapper);
    }

    // --------------------------------------------------
    // TEST 8: USER NOT FOUND
    // --------------------------------------------------

    @Test
    void processPayment_shouldThrowWhenAuthenticatedUserDoesNotExist() {

        setAuthenticationOnly();

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> paymentService.processPayment(
                        25,
                        "payment-key-008",
                        paymentRequest(true)
                )
        );

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(paymentMapper);
    }

    // --------------------------------------------------
    // TEST 9: ORDER NOT FOUND
    // --------------------------------------------------

    @Test
    void processPayment_shouldThrowWhenOrderDoesNotExistForUser() {

        authenticateUser();

        when(orderRepository.findByIdAndUserIdForUpdate(25, 10))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> paymentService.processPayment(
                        25,
                        "payment-key-009",
                        paymentRequest(true)
                )
        );

        verify(paymentRepository, never())
                .findByIdempotencyKey(anyString());

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verifyNoInteractions(paymentMapper);
    }

    // --------------------------------------------------
    // TEST 10: SAME IDEMPOTENCY KEY AND SAME REQUEST
    // --------------------------------------------------

    @Test
    void processPayment_shouldReturnExistingPaymentForSameRequest() {

        authenticateUser();
        prepareOrderLookup();

        Payment existingPayment = Payment.builder()
                .id(100)
                .order(order)
                .idempotencyKey("payment-key-010")
                .amount(new BigDecimal("125.50"))
                .status(PaymentStatus.SUCCEEDED)
                .simulatedSuccess(true)
                .build();

        when(paymentRepository.findByIdempotencyKey("payment-key-010"))
                .thenReturn(Optional.of(existingPayment));

        when(paymentMapper.toResponse(existingPayment))
                .thenReturn(paymentResponse);

        PaymentResponse result = paymentService.processPayment(
                25,
                "payment-key-010",
                paymentRequest(true)
        );

        assertNotNull(result);
        assertEquals("SUCCEEDED", result.getStatus());

        verify(paymentMapper).toResponse(existingPayment);

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verify(paymentRepository, never())
                .existsByOrderIdAndStatus(
                        anyInt(),
                        any(PaymentStatus.class)
                );
    }

    // --------------------------------------------------
    // TEST 11: SAME KEY, DIFFERENT ORDER
    // --------------------------------------------------

    @Test
    void processPayment_shouldRejectKeyUsedForDifferentOrder() {

        authenticateUser();
        prepareOrderLookup();

        Order anotherOrder = new Order();
        anotherOrder.setId(99);
        anotherOrder.setOrderNumber("ORD-OTHER-001");
        anotherOrder.setUser(user);
        anotherOrder.setStatus(OrderStatus.PENDING);
        anotherOrder.setTotalAmount(new BigDecimal("50.00"));

        Payment existingPayment = Payment.builder()
                .id(100)
                .order(anotherOrder)
                .idempotencyKey("payment-key-011")
                .amount(new BigDecimal("50.00"))
                .status(PaymentStatus.SUCCEEDED)
                .simulatedSuccess(true)
                .build();

        when(paymentRepository.findByIdempotencyKey("payment-key-011"))
                .thenReturn(Optional.of(existingPayment));

        assertThrows(
                ConflictException.class,
                () -> paymentService.processPayment(
                        25,
                        "payment-key-011",
                        paymentRequest(true)
                )
        );

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verifyNoInteractions(paymentMapper);
    }

    // --------------------------------------------------
    // TEST 12: SAME KEY, DIFFERENT REQUEST
    // --------------------------------------------------

    @Test
    void processPayment_shouldRejectKeyUsedForDifferentRequest() {

        authenticateUser();
        prepareOrderLookup();

        Payment existingPayment = Payment.builder()
                .id(100)
                .order(order)
                .idempotencyKey("payment-key-012")
                .amount(new BigDecimal("125.50"))
                .status(PaymentStatus.FAILED)
                .simulatedSuccess(false)
                .build();

        when(paymentRepository.findByIdempotencyKey("payment-key-012"))
                .thenReturn(Optional.of(existingPayment));

        assertThrows(
                ConflictException.class,
                () -> paymentService.processPayment(
                        25,
                        "payment-key-012",
                        paymentRequest(true)
                )
        );

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verifyNoInteractions(paymentMapper);
    }

    // --------------------------------------------------
    // TEST 13: ORDER IS NOT PENDING
    // --------------------------------------------------

    @Test
    void processPayment_shouldRejectNonPendingOrder() {

        authenticateUser();

        order.setStatus(OrderStatus.CONFIRMED);

        prepareOrderLookup();

        when(paymentRepository.findByIdempotencyKey("payment-key-013"))
                .thenReturn(Optional.empty());

        assertThrows(
                ConflictException.class,
                () -> paymentService.processPayment(
                        25,
                        "payment-key-013",
                        paymentRequest(true)
                )
        );

        verify(paymentRepository, never())
                .existsByOrderIdAndStatus(
                        anyInt(),
                        any(PaymentStatus.class)
                );

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verifyNoInteractions(paymentMapper);
    }

    // --------------------------------------------------
    // TEST 14: ORDER ALREADY HAS SUCCESSFUL PAYMENT
    // --------------------------------------------------

    @Test
    void processPayment_shouldRejectAlreadyPaidOrder() {

        authenticateUser();
        prepareOrderLookup();

        when(paymentRepository.findByIdempotencyKey("payment-key-014"))
                .thenReturn(Optional.empty());

        when(paymentRepository.existsByOrderIdAndStatus(
                25, PaymentStatus.SUCCEEDED))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> paymentService.processPayment(
                        25,
                        "payment-key-014",
                        paymentRequest(true)
                )
        );

        verify(paymentRepository).existsByOrderIdAndStatus(
                25,
                PaymentStatus.SUCCEEDED
        );

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verifyNoInteractions(paymentMapper);
    }
}
