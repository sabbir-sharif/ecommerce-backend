package com.ecommerce.inventory.service;

import com.ecommerce.user.entity.User;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.entity.InventoryMovement;
import com.ecommerce.inventory.entity.InventoryMovementType;
import com.ecommerce.inventory.repository.InventoryMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryMovementService {

    private final InventoryMovementRepository movementRepository;

    public InventoryMovement record(
            Inventory inventory,
            InventoryMovementType movementType,
            int quantityBefore,
            int quantityAfter,
            int reservedBefore,
            int reservedAfter,
            String referenceType,
            Integer referenceId,
            String reason,
            User performedBy
    ) {
        InventoryMovement movement = InventoryMovement.builder()
                .inventory(inventory)
                .movementType(movementType)
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .reservedBefore(reservedBefore)
                .reservedAfter(reservedAfter)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .reason(reason)
                .performedBy(performedBy)
                .build();

        return movementRepository.save(movement);
    }
}
