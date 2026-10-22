package com.ecommerce.order.service;

import com.ecommerce.order.dto.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse placeOrder();

    List<OrderResponse> getMyOrders();

    OrderResponse getMyOrderById(int orderId);

    OrderResponse cancelMyOrder(int orderId);
}
