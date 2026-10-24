package com.ecommerce.order.repository;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findByUserIdOrderByCreatedAtDesc(int userId);

    Optional<Order> findByIdAndUserId(int id, int userId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT o
        FROM Order o
        WHERE o.id = :orderId
          AND o.user.id = :userId
        """)
    Optional<Order> findByIdAndUserIdForUpdate(
            @Param("orderId") int orderId,
            @Param("userId") int userId
    );

    // for admin work
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :orderId")
    Optional<Order> findByIdForUpdate(
            @Param("orderId") int orderId
    );

    Page<Order> findByUserIdOrderByCreatedAtDesc(
            int userId,
            Pageable pageable
    );

    Page<Order> findByUserIdAndStatusOrderByCreatedAtDesc(
            int userId,
            OrderStatus status,
            Pageable pageable
    );

    Page<Order> findAllByOrderByCreatedAtDesc(
            Pageable pageable
    );

    Page<Order> findByStatusOrderByCreatedAtDesc(
            OrderStatus status,
            Pageable pageable
    );

}
