package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.InventoryCreateRequest;
import com.ecommerce.inventory.dto.InventoryResponse;

import java.util.List;

public interface InventoryService {

    InventoryResponse create(InventoryCreateRequest request);

    InventoryResponse getById(int id);

    InventoryResponse getByProductId(int productId);

    List<InventoryResponse> getAll();

    InventoryResponse updateStock(int id, int quantity);
}