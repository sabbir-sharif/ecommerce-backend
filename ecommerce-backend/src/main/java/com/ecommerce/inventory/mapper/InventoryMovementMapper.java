package com.ecommerce.inventory.mapper;

import com.ecommerce.inventory.dto.InventoryMovementResponse;
import com.ecommerce.inventory.entity.InventoryMovement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryMovementMapper {

    @Mapping(target = "inventoryId", source = "inventory.id")
    @Mapping(target = "productId", source = "inventory.product.id")
    @Mapping(target = "productName", source = "inventory.product.name")
    @Mapping(
            target = "performedByUserId",
            source = "performedBy.id"
    )
    InventoryMovementResponse toResponse(InventoryMovement movement);
}
