package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.entity.InventoryMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository
        extends JpaRepository<InventoryMovement, Integer> {

    Page<InventoryMovement> findByInventoryIdOrderByCreatedAtDesc(
            int inventoryId,
            Pageable pageable
    );

    Page<InventoryMovement> findAllByOrderByCreatedAtDesc(
            Pageable pageable
    );

    Page<InventoryMovement>
    findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(
            String referenceType,
            Integer referenceId,
            Pageable pageable
    );
}
