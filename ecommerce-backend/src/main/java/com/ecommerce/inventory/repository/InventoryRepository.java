package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryRepository extends
        JpaRepository<Inventory, Integer> {

    Optional<Inventory> findByProductId(int productId);

    boolean existsByProductId(int productId);
}
