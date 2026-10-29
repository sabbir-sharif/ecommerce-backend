package com.ecommerce.inventory.controller;

import com.ecommerce.common.response.ApiResponse;
import com.ecommerce.inventory.dto.InventoryMovementResponse;
import com.ecommerce.inventory.entity.InventoryMovement;
import com.ecommerce.inventory.mapper.InventoryMovementMapper;
import com.ecommerce.inventory.repository.InventoryMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory/movements")
@RequiredArgsConstructor
public class InventoryMovementController {

    private final InventoryMovementRepository movementRepository;
    private final InventoryMovementMapper movementMapper;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<InventoryMovementResponse>> getAllMovements(
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        Page<InventoryMovementResponse> response =
                movementRepository
                        .findAllByOrderByCreatedAtDesc(pageable)
                        .map(movementMapper::toResponse);

        return ApiResponse.success(
                "Inventory movement history retrieved successfully",
                response
        );
    }

    @GetMapping("/inventory/{inventoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<InventoryMovementResponse>>
    getMovementsByInventory(
            @PathVariable int inventoryId,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        Page<InventoryMovementResponse> response =
                movementRepository
                        .findByInventoryIdOrderByCreatedAtDesc(
                                inventoryId,
                                pageable
                        )
                        .map(movementMapper::toResponse);

        return ApiResponse.success(
                "Inventory movement history retrieved successfully",
                response
        );
    }
}
