package com.ecommerce.order.service;

import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {

    OrderResponse placeOrder();

    //List<OrderResponse> getMyOrders();
    Page<OrderResponse> getMyOrders(
            OrderStatus status,
            Pageable pageable
    );

    Page<OrderResponse> getAllOrders(
            OrderStatus status,
            Pageable pageable
    );

    OrderResponse getMyOrderById(int orderId);

    OrderResponse cancelMyOrder(int orderId);

    OrderResponse confirmOrder(int orderId);

    OrderResponse fulfillOrder(int orderId);
}
