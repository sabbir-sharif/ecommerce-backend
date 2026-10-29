package com.ecommerce.inventory.dto;

import com.ecommerce.inventory.entity.InventoryMovementType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryMovementResponse {

    private int id;
    private int inventoryId;
    private int productId;
    private String productName;

    private InventoryMovementType movementType;

    private int quantityBefore;
    private int quantityAfter;

    private int reservedBefore;
    private int reservedAfter;

    private String referenceType;
    private Integer referenceId;
    private String reason;

    private Integer performedByUserId;
    private LocalDateTime createdAt;
}
