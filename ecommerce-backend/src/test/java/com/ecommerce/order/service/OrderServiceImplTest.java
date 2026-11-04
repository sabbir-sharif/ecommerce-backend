package com.ecommerce.order.service;

import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartItemRepository;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.entity.InventoryMovementType;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.inventory.service.InventoryMovementService;
import com.ecommerce.order.dto.OrderItemResponse;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.repository.OrderItemRepository;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.repository.ProductRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private PaymentRepository paymentRepository;
    @Mock private InventoryMovementService inventoryMovementService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private Product product;
    private Inventory inventory;
    private Cart cart;
    private CartItem cartItem;
    private Order order;
    private OrderItem orderItem;
    private OrderResponse orderResponse;
    private OrderItemResponse orderItemResponse;

    @BeforeEach
    void setUp() {

        user = new User();
        user.setId(1);
        user.setName("Test User");
        user.setEmail("test@example.com");

        product = new Product();
        product.setId(100);
        product.setName("Laptop");
        product.setSku("LAP-100");
        product.setPrice(new BigDecimal("50000.00"));
        product.setStatus(ProductStatus.ACTIVE);

        inventory = new Inventory();
        inventory.setId(20);
        inventory.setProduct(product);
        inventory.setQuantity(20);
        inventory.setReservedQuantity(5);

        cart = new Cart();
        cart.setId(10);
        cart.setUser(user);

        cartItem = new CartItem();
        cartItem.setId(30);
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);

        order = new Order();
        order.setId(40);
        order.setOrderNumber("ORD-TEST-001");
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("100000.00"));

        orderItem = new OrderItem();
        orderItem.setId(50);
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setProductName(product.getName());
        orderItem.setSku(product.getSku());
        orderItem.setUnitPrice(product.getPrice());
        orderItem.setQuantity(2);
        orderItem.setSubtotal(new BigDecimal("100000.00"));

        orderResponse = OrderResponse.builder()
                .orderId(40)
                .orderNumber("ORD-TEST-001")
                .status("PENDING")
                .totalAmount(new BigDecimal("100000.00"))
                .build();

        orderItemResponse = OrderItemResponse.builder()
                .orderItemId(50)
                .productId(100)
                .productName("Laptop")
                .sku("LAP-100")
                .unitPrice(new BigDecimal("50000.00"))
                .quantity(2)
                .subtotal(new BigDecimal("100000.00"))
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // AUTHENTICATION HELPER

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

    private void prepareOrderItemMapping() {

        when(orderItemRepository.findByOrderId(order.getId()))
                .thenReturn(List.of(orderItem));

        when(orderMapper.toOrderItemResponse(orderItem))
                .thenReturn(orderItemResponse);

        when(orderMapper.toOrderResponse(any(Order.class)))
                .thenReturn(orderResponse);
    }

    private void prepareOrderItemsForCart() {

        authenticateUser();

        when(cartRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByCartId(cart.getId()))
                .thenReturn(List.of(cartItem));

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        when(inventoryRepository.findByProductId(product.getId()))
                .thenReturn(Optional.of(inventory));

        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(orderItemRepository.save(any(OrderItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(orderItemRepository.findByOrderId(anyInt()))
                .thenReturn(List.of());

        when(orderMapper.toOrderResponse(any(Order.class)))
                .thenReturn(orderResponse);
    }

    private void prepareExistingOrder() {

        authenticateUser();

        when(orderRepository.findByIdForUpdate(order.getId()))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(orderItemRepository.findByOrderId(order.getId()))
                .thenReturn(List.of(orderItem));

        when(orderMapper.toOrderItemResponse(orderItem))
                .thenReturn(orderItemResponse);

        when(orderMapper.toOrderResponse(any(Order.class)))
                .thenReturn(orderResponse);
    }

    // 1. PLACE ORDER TESTS
    @Test
    void placeOrder_shouldCreateOrderSuccessfully() {

        prepareOrderItemsForCart();

        OrderResponse result = orderService.placeOrder();

        assertNotNull(result);

        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).save(any(OrderItem.class));
        verify(cartItemRepository).deleteByCartId(cart.getId());
    }

    @Test
    void placeOrder_shouldThrowWhenCartDoesNotExist() {

        authenticateUser();

        when(cartRepository.findByUserId(user.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                ConflictException.class,
                () -> orderService.placeOrder()
        );

        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_shouldThrowWhenCartIsEmpty() {

        authenticateUser();

        when(cartRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByCartId(cart.getId()))
                .thenReturn(List.of());

        assertThrows(
                ConflictException.class,
                () -> orderService.placeOrder()
        );

        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_shouldThrowWhenProductDoesNotExist() {

        authenticateUser();

        when(cartRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByCartId(cart.getId()))
                .thenReturn(List.of(cartItem));

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.placeOrder()
        );

        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_shouldThrowWhenProductIsInactive() {

        authenticateUser();

        product.setStatus(ProductStatus.INACTIVE);

        when(cartRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByCartId(cart.getId()))
                .thenReturn(List.of(cartItem));

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        assertThrows(
                ConflictException.class,
                () -> orderService.placeOrder()
        );

        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_shouldThrowWhenInventoryDoesNotExist() {

        authenticateUser();

        when(cartRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByCartId(cart.getId()))
                .thenReturn(List.of(cartItem));

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        when(inventoryRepository.findByProductId(product.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.placeOrder()
        );

        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_shouldThrowWhenRequestedQuantityExceedsAvailableStock() {

        authenticateUser();

        cartItem.setQuantity(16);
        inventory.setQuantity(20);
        inventory.setReservedQuantity(5);

        when(cartRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByCartId(cart.getId()))
                .thenReturn(List.of(cartItem));

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        when(inventoryRepository.findByProductId(product.getId()))
                .thenReturn(Optional.of(inventory));

        assertThrows(
                ConflictException.class,
                () -> orderService.placeOrder()
        );

        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_shouldThrowWhenCartQuantityIsZero() {

        authenticateUser();
        cartItem.setQuantity(0);

        when(cartRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByCartId(cart.getId()))
                .thenReturn(List.of(cartItem));

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        when(inventoryRepository.findByProductId(product.getId()))
                .thenReturn(Optional.of(inventory));

        assertThrows(
                ConflictException.class,
                () -> orderService.placeOrder()
        );

        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_shouldCalculateTotalAmountCorrectly() {

        prepareOrderItemsForCart();

        orderService.placeOrder();

        ArgumentCaptor<Order> captor =
                ArgumentCaptor.forClass(Order.class);

        verify(orderRepository).save(captor.capture());

        assertEquals(
                new BigDecimal("100000.00"),
                captor.getValue().getTotalAmount()
        );
    }

    @Test
    void placeOrder_shouldReserveStockWithoutReducingPhysicalQuantity() {

        prepareOrderItemsForCart();

        orderService.placeOrder();

        assertEquals(20, inventory.getQuantity());
        assertEquals(7, inventory.getReservedQuantity());

        verify(inventoryMovementService).record(
                eq(inventory),
                eq(InventoryMovementType.STOCK_RESERVED),
                eq(20),
                eq(20),
                eq(5),
                eq(7),
                eq("ORDER"),
                any(),
                anyString(),
                eq(user)
        );
    }

    @Test
    void placeOrder_shouldSaveOrderItemSnapshot() {

        prepareOrderItemsForCart();

        orderService.placeOrder();

        ArgumentCaptor<OrderItem> captor =
                ArgumentCaptor.forClass(OrderItem.class);

        verify(orderItemRepository).save(captor.capture());

        OrderItem savedItem = captor.getValue();

        assertEquals("Laptop", savedItem.getProductName());
        assertEquals("LAP-100", savedItem.getSku());
        assertEquals(new BigDecimal("50000.00"), savedItem.getUnitPrice());
        assertEquals(2, savedItem.getQuantity());
        assertEquals(
                new BigDecimal("100000.00"),
                savedItem.getSubtotal()
        );
    }

    @Test
    void placeOrder_shouldClearCartAfterCreatingOrder() {

        prepareOrderItemsForCart();

        orderService.placeOrder();

        verify(cartItemRepository).deleteByCartId(cart.getId());
    }

    // 2. GET MY ORDERS TESTS
    @Test
    void getMyOrders_shouldReturnOrdersWithoutStatusFilter() {

        authenticateUser();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage =
                new PageImpl<>(List.of(order));

        when(orderRepository.findByUserIdOrderByCreatedAtDesc(
                user.getId(), pageable
        )).thenReturn(orderPage);

        prepareOrderItemMapping();

        Page<OrderResponse> result =
                orderService.getMyOrders(null, pageable);

        assertEquals(1, result.getTotalElements());

        verify(orderRepository)
                .findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
    }

    @Test
    void getMyOrders_shouldFilterByStatus() {

        authenticateUser();

        Pageable pageable = PageRequest.of(0, 10);

        when(orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                user.getId(), OrderStatus.PENDING, pageable
        )).thenReturn(new PageImpl<>(List.of(order)));

        prepareOrderItemMapping();

        Page<OrderResponse> result =
                orderService.getMyOrders(OrderStatus.PENDING, pageable);

        assertEquals(1, result.getTotalElements());

        verify(orderRepository).findByUserIdAndStatusOrderByCreatedAtDesc(
                user.getId(), OrderStatus.PENDING, pageable
        );
    }

    // 3. GET ALL ORDERS TESTS
    @Test
    void getAllOrders_shouldReturnAllOrdersWithoutStatusFilter() {

        Pageable pageable = PageRequest.of(0, 10);

        when(orderRepository.findAllByOrderByCreatedAtDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(order)));

        prepareOrderItemMapping();

        Page<OrderResponse> result =
                orderService.getAllOrders(null, pageable);

        assertEquals(1, result.getTotalElements());

        verify(orderRepository)
                .findAllByOrderByCreatedAtDesc(pageable);
    }

    @Test
    void getAllOrders_shouldFilterByStatus() {

        Pageable pageable = PageRequest.of(0, 10);

        when(orderRepository.findByStatusOrderByCreatedAtDesc(
                OrderStatus.PENDING, pageable
        )).thenReturn(new PageImpl<>(List.of(order)));

        prepareOrderItemMapping();

        Page<OrderResponse> result =
                orderService.getAllOrders(OrderStatus.PENDING, pageable);

        assertEquals(1, result.getTotalElements());

        verify(orderRepository).findByStatusOrderByCreatedAtDesc(
                OrderStatus.PENDING, pageable
        );
    }

    // 4. GET MY ORDER BY ID TESTS
    @Test
    void getMyOrderById_shouldReturnOrderSuccessfully() {

        authenticateUser();

        when(orderRepository.findByIdAndUserId(
                order.getId(), user.getId()
        )).thenReturn(Optional.of(order));

        prepareOrderItemMapping();

        OrderResponse result =
                orderService.getMyOrderById(order.getId());

        assertNotNull(result);

        verify(orderRepository)
                .findByIdAndUserId(order.getId(), user.getId());
    }

    @Test
    void getMyOrderById_shouldThrowWhenOrderDoesNotBelongToUserOrDoesNotExist() {

        authenticateUser();

        when(orderRepository.findByIdAndUserId(
                order.getId(), user.getId()
        )).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getMyOrderById(order.getId())
        );
    }

    // 5. CONFIRM ORDER TESTS
    @Test
    void confirmOrder_shouldConfirmPendingOrder() {

        prepareExistingOrder();

        OrderResponse result =
                orderService.confirmOrder(order.getId());

        assertNotNull(result);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());

        verify(orderRepository).save(order);
    }

    @Test
    void confirmOrder_shouldThrowWhenOrderDoesNotExist() {

        authenticateUser();

        when(orderRepository.findByIdForUpdate(order.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.confirmOrder(order.getId())
        );
    }

    @Test
    void confirmOrder_shouldThrowWhenOrderIsNotPending() {

        authenticateUser();
        order.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findByIdForUpdate(order.getId()))
                .thenReturn(Optional.of(order));

        assertThrows(
                ConflictException.class,
                () -> orderService.confirmOrder(order.getId())
        );

        verify(orderRepository, never()).save(any());
    }

    @Test
    void confirmOrder_shouldThrowWhenOrderHasNoItems() {

        authenticateUser();

        when(orderRepository.findByIdForUpdate(order.getId()))
                .thenReturn(Optional.of(order));

        when(orderItemRepository.findByOrderId(order.getId()))
                .thenReturn(List.of());

        assertThrows(
                ConflictException.class,
                () -> orderService.confirmOrder(order.getId())
        );
    }

    // 6. FULFILL ORDER TESTS
    @Test
    void fulfillOrder_shouldFulfillOrderAndDeductStock() {

        prepareExistingOrder();

        // Fulfillment requires a confirmed order.
        order.setStatus(OrderStatus.CONFIRMED);

        when(paymentRepository.existsByOrderIdAndStatus(
                order.getId(), PaymentStatus.SUCCEEDED
        )).thenReturn(true);

        when(inventoryRepository.findByProductId(product.getId()))
                .thenReturn(Optional.of(inventory));

        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse result =
                orderService.fulfillOrder(order.getId());

        assertNotNull(result);
        assertEquals(OrderStatus.FULFILLED, order.getStatus());

        // Physical stock: 20 - 2 = 18
        assertEquals(18, inventory.getQuantity());

        // Reserved stock: 5 - 2 = 3
        assertEquals(3, inventory.getReservedQuantity());

        verify(inventoryMovementService).record(
                eq(inventory),
                eq(InventoryMovementType.STOCK_FULFILLED),
                eq(20),
                eq(18),
                eq(5),
                eq(3),
                eq("ORDER"),
                eq(order.getId()),
                anyString(),
                eq(user)
        );
    }

    @Test
    void fulfillOrder_shouldThrowWhenOrderDoesNotExist() {

        authenticateUser();

        when(orderRepository.findByIdForUpdate(order.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.fulfillOrder(order.getId())
        );
    }

    @Test
    void fulfillOrder_shouldThrowWhenOrderIsNotConfirmed() {

        authenticateUser();
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findByIdForUpdate(order.getId()))
                .thenReturn(Optional.of(order));

        assertThrows(
                ConflictException.class,
                () -> orderService.fulfillOrder(order.getId())
        );

        verify(paymentRepository, never())
                .existsByOrderIdAndStatus(anyInt(), any());

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void fulfillOrder_shouldThrowWhenPaymentHasNotSucceeded() {

        authenticateUser();
        order.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findByIdForUpdate(order.getId()))
                .thenReturn(Optional.of(order));

        when(paymentRepository.existsByOrderIdAndStatus(
                order.getId(), PaymentStatus.SUCCEEDED
        )).thenReturn(false);

        assertThrows(
                ConflictException.class,
                () -> orderService.fulfillOrder(order.getId())
        );

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void fulfillOrder_shouldThrowWhenOrderHasNoItems() {

        authenticateUser();
        order.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findByIdForUpdate(order.getId()))
                .thenReturn(Optional.of(order));

        when(paymentRepository.existsByOrderIdAndStatus(
                order.getId(), PaymentStatus.SUCCEEDED
        )).thenReturn(true);

        when(orderItemRepository.findByOrderId(order.getId()))
                .thenReturn(List.of());

        assertThrows(
                ConflictException.class,
                () -> orderService.fulfillOrder(order.getId())
        );
    }

    @Test
    void fulfillOrder_shouldThrowWhenInventoryDoesNotExist() {

        authenticateUser();
        order.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findByIdForUpdate(order.getId()))
                .thenReturn(Optional.of(order));

        when(paymentRepository.existsByOrderIdAndStatus(
                order.getId(), PaymentStatus.SUCCEEDED
        )).thenReturn(true);

        when(orderItemRepository.findByOrderId(order.getId()))
                .thenReturn(List.of(orderItem));

        when(inventoryRepository.findByProductId(product.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.fulfillOrder(order.getId())
        );
    }

    @Test
    void fulfillOrder_shouldThrowWhenPhysicalStockIsInsufficient() {

        authenticateUser();
        order.setStatus(OrderStatus.CONFIRMED);
        inventory.setQuantity(1);

        when(orderRepository.findByIdForUpdate(order.getId()))
                .thenReturn(Optional.of(order));

        when(paymentRepository.existsByOrderIdAndStatus(
                order.getId(), PaymentStatus.SUCCEEDED
        )).thenReturn(true);

        when(orderItemRepository.findByOrderId(order.getId()))
                .thenReturn(List.of(orderItem));

        when(inventoryRepository.findByProductId(product.getId()))
                .thenReturn(Optional.of(inventory));

        assertThrows(
                ConflictException.class,
                () -> orderService.fulfillOrder(order.getId())
        );

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void fulfillOrder_shouldThrowWhenReservedStockIsInsufficient() {

        authenticateUser();
        order.setStatus(OrderStatus.CONFIRMED);
        inventory.setReservedQuantity(1);

        when(orderRepository.findByIdForUpdate(order.getId()))
                .thenReturn(Optional.of(order));

        when(paymentRepository.existsByOrderIdAndStatus(
                order.getId(), PaymentStatus.SUCCEEDED
        )).thenReturn(true);

        when(orderItemRepository.findByOrderId(order.getId()))
                .thenReturn(List.of(orderItem));

        when(inventoryRepository.findByProductId(product.getId()))
                .thenReturn(Optional.of(inventory));

        assertThrows(
                ConflictException.class,
                () -> orderService.fulfillOrder(order.getId())
        );

        verify(inventoryRepository, never()).save(any());
    }

    // 7. CANCEL ORDER TESTS
    @Test
    void cancelMyOrder_shouldCancelOrderAndReleaseReservedStock() {

        authenticateUser();

        when(orderRepository.findByIdAndUserIdForUpdate(
                order.getId(), user.getId()
        )).thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(orderItemRepository.findByOrderId(order.getId()))
                .thenReturn(List.of(orderItem));

        when(orderMapper.toOrderItemResponse(orderItem))
                .thenReturn(orderItemResponse);

        when(orderMapper.toOrderResponse(any(Order.class)))
                .thenReturn(orderResponse);

        when(inventoryRepository.findByProductId(product.getId()))
                .thenReturn(Optional.of(inventory));

        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse result =
                orderService.cancelMyOrder(order.getId());

        assertNotNull(result);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());

        // Physical stock must remain unchanged.
        assertEquals(20, inventory.getQuantity());

        // Reserved stock: 5 - 2 = 3
        assertEquals(3, inventory.getReservedQuantity());

        verify(inventoryMovementService).record(
                eq(inventory),
                eq(InventoryMovementType.STOCK_RELEASED),
                eq(20),
                eq(20),
                eq(5),
                eq(3),
                eq("ORDER"),
                eq(order.getId()),
                anyString(),
                eq(user)
        );
    }

    @Test
    void cancelMyOrder_shouldThrowWhenOrderDoesNotExistOrBelongsToAnotherUser() {

        authenticateUser();

        when(orderRepository.findByIdAndUserIdForUpdate(
                order.getId(), user.getId()
        )).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.cancelMyOrder(order.getId())
        );
    }

    @Test
    void cancelMyOrder_shouldThrowWhenOrderIsNotPending() {

        authenticateUser();
        order.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findByIdAndUserIdForUpdate(
                order.getId(), user.getId()
        )).thenReturn(Optional.of(order));

        assertThrows(
                ConflictException.class,
                () -> orderService.cancelMyOrder(order.getId())
        );

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void cancelMyOrder_shouldThrowWhenOrderHasNoItems() {

        authenticateUser();

        when(orderRepository.findByIdAndUserIdForUpdate(
                order.getId(), user.getId()
        )).thenReturn(Optional.of(order));

        when(orderItemRepository.findByOrderId(order.getId()))
                .thenReturn(List.of());

        assertThrows(
                ConflictException.class,
                () -> orderService.cancelMyOrder(order.getId())
        );
    }

    @Test
    void cancelMyOrder_shouldThrowWhenInventoryDoesNotExist() {

        authenticateUser();

        when(orderRepository.findByIdAndUserIdForUpdate(
                order.getId(), user.getId()
        )).thenReturn(Optional.of(order));

        when(orderItemRepository.findByOrderId(order.getId()))
                .thenReturn(List.of(orderItem));

        when(inventoryRepository.findByProductId(product.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.cancelMyOrder(order.getId())
        );
    }

    @Test
    void cancelMyOrder_shouldThrowWhenReservedStockIsInsufficient() {

        authenticateUser();
        inventory.setReservedQuantity(1);

        when(orderRepository.findByIdAndUserIdForUpdate(
                order.getId(), user.getId()
        )).thenReturn(Optional.of(order));

        when(orderItemRepository.findByOrderId(order.getId()))
                .thenReturn(List.of(orderItem));

        when(inventoryRepository.findByProductId(product.getId()))
                .thenReturn(Optional.of(inventory));

        assertThrows(
                ConflictException.class,
                () -> orderService.cancelMyOrder(order.getId())
        );

        verify(inventoryRepository, never()).save(any());
    }
}