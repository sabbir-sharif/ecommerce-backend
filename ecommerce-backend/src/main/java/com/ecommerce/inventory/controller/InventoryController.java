package com.ecommerce.inventory.controller;

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
    public ResponseEntity<InventoryResponse> create(
            @Valid @RequestBody InventoryCreateRequest request) {

        InventoryResponse response =
                inventoryService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryResponse> getById(
            @PathVariable int id) {

        InventoryResponse response =
                inventoryService.getById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryResponse> getByProductId(
            @PathVariable int productId) {

        InventoryResponse response =
                inventoryService.getByProductId(productId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getAll() {

        List<InventoryResponse> response =
                inventoryService.getAll();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/stock")
    public ResponseEntity<InventoryResponse> updateStock(
            @PathVariable int id,
            @RequestParam int quantity) {

        InventoryResponse response =
                inventoryService.updateStock(id, quantity);

        return ResponseEntity.ok(response);
    }
}
