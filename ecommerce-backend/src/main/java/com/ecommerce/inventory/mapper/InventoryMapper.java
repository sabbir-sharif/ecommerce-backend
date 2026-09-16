package com.ecommerce.inventory.mapper;

import com.ecommerce.inventory.dto.InventoryCreateRequest;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.entity.Inventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "reservedQuantity", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Inventory toEntity(InventoryCreateRequest request);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(
            target = "availableQuantity",
            expression = "java(inventory.getQuantity() - inventory.getReservedQuantity())"
    )
    InventoryResponse toResponse(Inventory inventory);
}
