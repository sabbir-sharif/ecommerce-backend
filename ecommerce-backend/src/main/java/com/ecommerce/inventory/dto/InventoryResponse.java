package com.ecommerce.inventory.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InventoryResponse {

    private int id;

    private int productId;

    private int quantity;

    private int reservedQuantity;

    private int availableQuantity;

    private int version;
}
