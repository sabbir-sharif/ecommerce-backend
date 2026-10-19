package com.ecommerce.cart.repository;

import com.ecommerce.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {

    List<CartItem> findByCartId(int cartId);

    Optional<CartItem> findByCartIdAndProductId(int cartId, int productId);

    boolean existsByCartIdAndProductId(int cartId, int productId);

    void deleteByCartIdAndProductId(int cartId, int productId);

    void deleteByCartId(int cartId);
}

