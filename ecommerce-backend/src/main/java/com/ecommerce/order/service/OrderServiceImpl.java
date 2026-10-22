package com.ecommerce.order.service;

import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartItemRepository;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.order.dto.OrderItemResponse;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.repository.OrderItemRepository;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
            inventory.setReservedQuantity(
                    inventory.getReservedQuantity()
                            + cartItem.getQuantity());

            // The Inventory @Version field protects concurrent updates.
            inventoryRepository.save(inventory);

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

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders() {

        User user = getAuthenticatedUser();

        return orderRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(order -> {
                    List<OrderItemResponse> items =
                            orderItemRepository.findByOrderId(order.getId())
                                    .stream()
                                    .map(orderMapper::toOrderItemResponse)
                                    .toList();

                    return mapOrder(order, items);
                })
                .toList();
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
    public OrderResponse cancelMyOrder(int orderId) {

        User user = getAuthenticatedUser();

        // Find and lock an order owned by the authenticated user.
        Order order = orderRepository
                .findByIdAndUserIdForUpdate(orderId, user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found"));

        // Only PENDING orders can be cancelled in Phase 3.6.
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException(
                    "Only pending orders can be cancelled");
        }

        // Load the items whose reserved stock must be released.
        List<OrderItem> orderItems =
                orderItemRepository.findByOrderId(order.getId());

        if (orderItems.isEmpty()) {
            throw new ConflictException(
                    "Cannot cancel an order without items");
        }

        // Release reserved stock for every order item.
        for (OrderItem orderItem : orderItems) {

            Inventory inventory = inventoryRepository
                    .findByProductId(orderItem.getProduct().getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Inventory not found for product: "
                                            + orderItem.getProductName()));

            int reservedQuantity = inventory.getReservedQuantity();
            int orderedQuantity = orderItem.getQuantity();

            // Never allow reservation to become negative.
            if (orderedQuantity <= 0
                    || reservedQuantity < orderedQuantity) {
                throw new ConflictException(
                        "Cannot safely release reserved stock for product: "
                                + orderItem.getProductName());
            }

            inventory.setReservedQuantity(
                    reservedQuantity - orderedQuantity);

            inventoryRepository.save(inventory);
        }

        // Mark the order cancelled only after all releases are valid.
        order.setStatus(OrderStatus.CANCELLED);

        Order savedOrder = orderRepository.save(order);

        // Return the updated order with its original item snapshots.
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
