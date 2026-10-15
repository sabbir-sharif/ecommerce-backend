package com.ecommerce.inventory.controller;

import com.ecommerce.common.response.ApiResponse;
import com.ecommerce.inventory.dto.InventoryCreateRequest;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.service.InventoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryResponse>> create(
            @Valid @RequestBody InventoryCreateRequest request) {

        InventoryResponse response = inventoryService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Inventory created successfully",
                        response
                ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getById(
            @PathVariable int id) {

        InventoryResponse response = inventoryService.getById(id);

        return ResponseEntity.ok(ApiResponse.success(
                "Inventory retrieved successfully",
                response
        ));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getByProductId(
            @PathVariable int productId) {

        InventoryResponse response =
                inventoryService.getByProductId(productId);

        return ResponseEntity.ok(ApiResponse.success(
                "Inventory retrieved successfully",
                response
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getAll() {

        List<InventoryResponse> response = inventoryService.getAll();

        return ResponseEntity.ok(ApiResponse.success(
                "Inventory records retrieved successfully",
                response
        ));
    }

    @PutMapping("/{id}/stock")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateStock(
            @PathVariable int id,
            @RequestParam int quantity) {

        InventoryResponse response =
                inventoryService.updateStock(id, quantity);

        return ResponseEntity.ok(ApiResponse.success(
                "Stock updated successfully",
                response
        ));
    }
}

