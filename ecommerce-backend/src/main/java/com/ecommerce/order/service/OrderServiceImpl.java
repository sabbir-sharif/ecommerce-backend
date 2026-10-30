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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderMapper orderMapper;
    private final PaymentRepository paymentRepository;
    private final InventoryMovementService inventoryMovementService;

    @Override
    @Transactional
    public OrderResponse placeOrder() {

        User user = getAuthenticatedUser();

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() ->
                        new ConflictException("Your cart is empty"));

        List<CartItem> cartItems =
                cartItemRepository.findByCartId(cart.getId());

        if (cartItems.isEmpty()) {
            throw new ConflictException("Your cart is empty");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        // Validate products and stock; calculate the total.
        for (CartItem cartItem : cartItems) {

            Product product = productRepository
                    .findById(cartItem.getProduct().getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Product not found"));

            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new ConflictException(
                        "Product is not active: " + product.getName());
            }

            Inventory inventory = inventoryRepository
                    .findByProductId(product.getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Inventory not found for product: "
                                            + product.getName()));

            int availableQuantity =
                    inventory.getQuantity()
                            - inventory.getReservedQuantity();

            if (cartItem.getQuantity() <= 0
                    || cartItem.getQuantity() > availableQuantity) {
                throw new ConflictException(
                        "Insufficient stock for product: "
                                + product.getName());
            }

            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(
                            cartItem.getQuantity()));

            totalAmount = totalAmount.add(subtotal);
        }

        // Create the order header.
        Order order = new Order();
        order.setOrderNumber("ORD-" + UUID.randomUUID());
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        // Reserve stock and save each order-item snapshot.
        for (CartItem cartItem : cartItems) {

            Product product = productRepository
                    .findById(cartItem.getProduct().getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Product not found"));

            Inventory inventory = inventoryRepository
                    .findByProductId(product.getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Inventory not found for product: "
                                            + product.getName()));

            // Reserve stock without decreasing physical quantity.
//            inventory.setReservedQuantity(
//                    inventory.getReservedQuantity()
//                            + cartItem.getQuantity());
//
//            // The Inventory @Version field protects concurrent updates.
//            inventoryRepository.save(inventory);


            int quantityBefore = inventory.getQuantity();
            int reservedBefore = inventory.getReservedQuantity();

// Reserve stock for this order.
// Physical quantity does not change.
            inventory.setReservedQuantity(
                    reservedBefore + cartItem.getQuantity()
            );

            Inventory updatedInventory = inventoryRepository.save(inventory);

// Record the reservation in the same transaction.
            inventoryMovementService.record(
                    updatedInventory,
                    InventoryMovementType.STOCK_RESERVED,
                    quantityBefore,
                    updatedInventory.getQuantity(),
                    reservedBefore,
                    updatedInventory.getReservedQuantity(),
                    "ORDER",
                    order.getId(),
                    "Stock reserved for order placement",
                    user
            );


            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setSku(product.getSku());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSubtotal(
                    product.getPrice().multiply(
                            BigDecimal.valueOf(
                                    cartItem.getQuantity())));

            orderItemRepository.save(orderItem);
        }

        // Clear the cart in the same transaction.
        cartItemRepository.deleteByCartId(cart.getId());

        // Build the response.
        List<OrderItemResponse> itemResponses =
                orderItemRepository.findByOrderId(savedOrder.getId())
                        .stream()
                        .map(orderMapper::toOrderItemResponse)
                        .toList();

        return mapOrder(savedOrder, itemResponses);
    }

//    @Override
//    @Transactional(readOnly = true)
//    public List<OrderResponse> getMyOrders() {
//
//        User user = getAuthenticatedUser();
//
//        return orderRepository
//                .findByUserIdOrderByCreatedAtDesc(user.getId())
//                .stream()
//                .map(order -> {
//                    List<OrderItemResponse> items =
//                            orderItemRepository.findByOrderId(order.getId())
//                                    .stream()
//                                    .map(orderMapper::toOrderItemResponse)
//                                    .toList();
//
//                    return mapOrder(order, items);
//                })
//                .toList();
//    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(
            OrderStatus status,
            Pageable pageable) {

        User user = getAuthenticatedUser();

        Page<Order> orders;

        if (status == null) {
            orders = orderRepository
                    .findByUserIdOrderByCreatedAtDesc(
                            user.getId(), pageable);
        } else {
            orders = orderRepository
                    .findByUserIdAndStatusOrderByCreatedAtDesc(
                            user.getId(), status, pageable);
        }

        return orders.map(this::mapOrderWithItems);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(
            OrderStatus status,
            Pageable pageable) {

        Page<Order> orders;

        if (status == null) {
            orders = orderRepository
                    .findAllByOrderByCreatedAtDesc(pageable);
        } else {
            orders = orderRepository
                    .findByStatusOrderByCreatedAtDesc(
                            status, pageable);
        }

        return orders.map(this::mapOrderWithItems);
    }

    private OrderResponse mapOrderWithItems(Order order) {

        List<OrderItemResponse> itemResponses =
                orderItemRepository.findByOrderId(order.getId())
                        .stream()
                        .map(orderMapper::toOrderItemResponse)
                        .toList();

        return mapOrder(order, itemResponses);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrderById(int orderId) {

        User user = getAuthenticatedUser();

        Order order = orderRepository
                .findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found"));

        List<OrderItemResponse> items =
                orderItemRepository.findByOrderId(order.getId())
                        .stream()
                        .map(orderMapper::toOrderItemResponse)
                        .toList();

        return mapOrder(order, items);
    }

    @Override
    @Transactional
    public OrderResponse confirmOrder(int orderId) {
        // Make sure the request is authenticated and the user exists.
        getAuthenticatedUser();

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException(
                    "Only pending orders can be confirmed");
        }

        List<OrderItem> orderItems =
                orderItemRepository.findByOrderId(order.getId());

        if (orderItems.isEmpty()) {
            throw new ConflictException(
                    "Cannot confirm an order without items");
        }

        // Confirming does not deduct or release inventory.
        order.setStatus(OrderStatus.CONFIRMED);
        Order savedOrder = orderRepository.save(order);

        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(orderMapper::toOrderItemResponse)
                .toList();

        return mapOrder(savedOrder, itemResponses);
    }

//    @Override
//    @Transactional
//    public OrderResponse fulfillOrder(int orderId) {
//        // Make sure the request is authenticated and the user exists.
//        getAuthenticatedUser();
//
//        Order order = orderRepository.findByIdForUpdate(orderId)
//                .orElseThrow(() ->
//                        new ResourceNotFoundException("Order not found"));
//
//        if (order.getStatus() != OrderStatus.CONFIRMED) {
//            throw new ConflictException(
//                    "Only confirmed orders can be fulfilled");
//        }
//
//        if (!paymentRepository.existsByOrderIdAndStatus(
//                order.getId(), PaymentStatus.SUCCEEDED)) {
//            throw new ConflictException(
//                    "Order cannot be fulfilled without successful payment");
//        }
//
//        List<OrderItem> orderItems =
//                orderItemRepository.findByOrderId(order.getId());
//
//        if (orderItems.isEmpty()) {
//            throw new ConflictException(
//                    "Cannot fulfill an order without items");
//        }
//
//        for (OrderItem orderItem : orderItems) {
//            Inventory inventory = inventoryRepository
//                    .findByProductId(orderItem.getProduct().getId())
//                    .orElseThrow(() ->
//                            new ResourceNotFoundException(
//                                    "Inventory not found for product: "
//                                            + orderItem.getProductName()));
//
//            int orderedQuantity = orderItem.getQuantity();
//
//            if (orderedQuantity <= 0) {
//                throw new ConflictException(
//                        "Invalid ordered quantity for product: "
//                                + orderItem.getProductName());
//            }
//
//            if (inventory.getQuantity() < orderedQuantity) {
//                throw new ConflictException(
//                        "Insufficient physical stock for product: "
//                                + orderItem.getProductName());
//            }
//
//            if (inventory.getReservedQuantity() < orderedQuantity) {
//                throw new ConflictException(
//                        "Insufficient reserved stock for product: "
//                                + orderItem.getProductName());
//            }
//
//            // Fulfillment is the point of physical stock deduction.
//            inventory.setQuantity(
//                    inventory.getQuantity() - orderedQuantity);
//
//            // Release the reservation for the fulfilled units.
//            inventory.setReservedQuantity(
//                    inventory.getReservedQuantity() - orderedQuantity);
//
//            inventoryRepository.save(inventory);
//        }
//
//        // Update the order only after all inventory changes are processed.
//        order.setStatus(OrderStatus.FULFILLED);
//        Order savedOrder = orderRepository.save(order);
//
//        List<OrderItemResponse> itemResponses = orderItems.stream()
//                .map(orderMapper::toOrderItemResponse)
//                .toList();
//
//        return mapOrder(savedOrder, itemResponses);
//    }

    @Override
    @Transactional
    public OrderResponse fulfillOrder(int orderId) {

        // 1. Make sure the request is authenticated.
        getAuthenticatedUser();

        // 2. Lock the order to prevent duplicate fulfillment.
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found"));

        // 3. Only CONFIRMED orders can be fulfilled.
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new ConflictException(
                    "Only confirmed orders can be fulfilled");
        }

        // 4. Require successful payment before deducting stock.
        if (!paymentRepository.existsByOrderIdAndStatus(
                order.getId(), PaymentStatus.SUCCEEDED)) {
            throw new ConflictException(
                    "Order cannot be fulfilled without successful payment");
        }

        // 5. Load order items.
        List<OrderItem> orderItems =
                orderItemRepository.findByOrderId(order.getId());

        if (orderItems.isEmpty()) {
            throw new ConflictException(
                    "Cannot fulfill an order without items");
        }

        // 6. Deduct physical stock, release reservations, and audit changes.
        for (OrderItem orderItem : orderItems) {

            Inventory inventory = inventoryRepository
                    .findByProductId(orderItem.getProduct().getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Inventory not found for product: "
                                            + orderItem.getProductName()));

            int orderedQuantity = orderItem.getQuantity();

            if (orderedQuantity <= 0) {
                throw new ConflictException(
                        "Invalid ordered quantity for product: "
                                + orderItem.getProductName());
            }

            int quantityBefore = inventory.getQuantity();
            int reservedBefore = inventory.getReservedQuantity();

            if (quantityBefore < orderedQuantity) {
                throw new ConflictException(
                        "Insufficient physical stock for product: "
                                + orderItem.getProductName());
            }

            if (reservedBefore < orderedQuantity) {
                throw new ConflictException(
                        "Insufficient reserved stock for product: "
                                + orderItem.getProductName());
            }

            // Physical stock is deducted at fulfillment.
            inventory.setQuantity(
                    quantityBefore - orderedQuantity);

            // Release the reservation for these fulfilled units.
            inventory.setReservedQuantity(
                    reservedBefore - orderedQuantity);

            Inventory updatedInventory =
                    inventoryRepository.save(inventory);

            // Record the inventory movement in the same transaction.
            inventoryMovementService.record(
                    updatedInventory,
                    InventoryMovementType.STOCK_FULFILLED,
                    quantityBefore,
                    updatedInventory.getQuantity(),
                    reservedBefore,
                    updatedInventory.getReservedQuantity(),
                    "ORDER",
                    order.getId(),
                    "Stock deducted during order fulfillment",
                    getAuthenticatedUser()
            );
        }

        // 7. Mark the order fulfilled after processing all items.
        order.setStatus(OrderStatus.FULFILLED);

        Order savedOrder = orderRepository.save(order);

        // 8. Return the order with its item snapshots.
        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(orderMapper::toOrderItemResponse)
                .toList();

        return mapOrder(savedOrder, itemResponses);
    }

//    @Override
//    @Transactional
//    public OrderResponse cancelMyOrder(int orderId) {
//
//        User user = getAuthenticatedUser();
//
//        // Find and lock an order owned by the authenticated user.
//        Order order = orderRepository
//                .findByIdAndUserIdForUpdate(orderId, user.getId())
//                .orElseThrow(() ->
//                        new ResourceNotFoundException("Order not found"));
//
//        // Only PENDING orders can be cancelled in Phase 3.6.
//        if (order.getStatus() != OrderStatus.PENDING) {
//            throw new ConflictException(
//                    "Only pending orders can be cancelled");
//        }
//
//        // Load the items whose reserved stock must be released.
//        List<OrderItem> orderItems =
//                orderItemRepository.findByOrderId(order.getId());
//
//        if (orderItems.isEmpty()) {
//            throw new ConflictException(
//                    "Cannot cancel an order without items");
//        }
//
//        // Release reserved stock for every order item.
//        for (OrderItem orderItem : orderItems) {
//
//            Inventory inventory = inventoryRepository
//                    .findByProductId(orderItem.getProduct().getId())
//                    .orElseThrow(() ->
//                            new ResourceNotFoundException(
//                                    "Inventory not found for product: "
//                                            + orderItem.getProductName()));
//
//            int reservedQuantity = inventory.getReservedQuantity();
//            int orderedQuantity = orderItem.getQuantity();
//
//            // Never allow reservation to become negative.
//            if (orderedQuantity <= 0
//                    || reservedQuantity < orderedQuantity) {
//                throw new ConflictException(
//                        "Cannot safely release reserved stock for product: "
//                                + orderItem.getProductName());
//            }
//
//            inventory.setReservedQuantity(
//                    reservedQuantity - orderedQuantity);
//
//            inventoryRepository.save(inventory);
//        }
//
//        // Mark the order cancelled only after all releases are valid.
//        order.setStatus(OrderStatus.CANCELLED);
//
//        Order savedOrder = orderRepository.save(order);
//
//        // Return the updated order with its original item snapshots.
//        List<OrderItemResponse> itemResponses =
//                orderItemRepository.findByOrderId(savedOrder.getId())
//                        .stream()
//                        .map(orderMapper::toOrderItemResponse)
//                        .toList();
//
//        return mapOrder(savedOrder, itemResponses);
//    }

    @Override
    @Transactional
    public OrderResponse cancelMyOrder(int orderId) {

        User user = getAuthenticatedUser();

        // 1. Find and lock an order owned by the authenticated user.
        Order order = orderRepository
                .findByIdAndUserIdForUpdate(orderId, user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found"));

        // 2. Only PENDING orders can be cancelled.
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException(
                    "Only pending orders can be cancelled");
        }

        // 3. Load the order items.
        List<OrderItem> orderItems =
                orderItemRepository.findByOrderId(order.getId());

        if (orderItems.isEmpty()) {
            throw new ConflictException(
                    "Cannot cancel an order without items");
        }

        // 4. Release reserved stock and record each movement.
        for (OrderItem orderItem : orderItems) {

            Inventory inventory = inventoryRepository
                    .findByProductId(orderItem.getProduct().getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Inventory not found for product: "
                                            + orderItem.getProductName()));

            int quantityBefore = inventory.getQuantity();
            int reservedBefore = inventory.getReservedQuantity();
            int orderedQuantity = orderItem.getQuantity();

            // Validate before changing inventory.
            if (orderedQuantity <= 0
                    || reservedBefore < orderedQuantity) {
                throw new ConflictException(
                        "Cannot safely release reserved stock for product: "
                                + orderItem.getProductName());
            }

            // Cancellation releases reserved stock only.
            // Physical quantity remains unchanged.
            inventory.setReservedQuantity(
                    reservedBefore - orderedQuantity);

            Inventory updatedInventory =
                    inventoryRepository.save(inventory);

            // Record the change in the same transaction.
            inventoryMovementService.record(
                    updatedInventory,
                    InventoryMovementType.STOCK_RELEASED,
                    quantityBefore,
                    updatedInventory.getQuantity(),
                    reservedBefore,
                    updatedInventory.getReservedQuantity(),
                    "ORDER",
                    order.getId(),
                    "Reserved stock released after order cancellation",
                    user
            );
        }

        // 5. Mark the order cancelled after processing its items.
        order.setStatus(OrderStatus.CANCELLED);

        Order savedOrder = orderRepository.save(order);

        // 6. Return the updated order with its original item snapshots.
        List<OrderItemResponse> itemResponses =
                orderItemRepository.findByOrderId(savedOrder.getId())
                        .stream()
                        .map(orderMapper::toOrderItemResponse)
                        .toList();

        return mapOrder(savedOrder, itemResponses);
    }


    private User getAuthenticatedUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            throw new ConflictException(
                    "Authentication is required");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated user not found"));
    }

    private OrderResponse mapOrder(
            Order order,
            List<OrderItemResponse> items) {

        OrderResponse response = orderMapper.toOrderResponse(order);
        response.setItems(items);

        return response;
    }
}
