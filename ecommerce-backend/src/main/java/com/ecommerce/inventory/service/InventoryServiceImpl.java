
package com.ecommerce.inventory.service;

import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.inventory.dto.InventoryCreateRequest;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.entity.InventoryMovementType;
import com.ecommerce.inventory.mapper.InventoryMapper;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final InventoryMapper inventoryMapper;
    private final InventoryMovementService inventoryMovementService;

    @Override
    @Transactional
    public InventoryResponse create(InventoryCreateRequest request) {

        if (inventoryRepository.existsByProductId(request.getProductId())) {
            throw new ConflictException(
                    "Inventory already exists for this product"
            );
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found"));

        Inventory inventory = inventoryMapper.toEntity(request);
        inventory.setProduct(product);
        inventory.setReservedQuantity(0);

        Inventory savedInventory = inventoryRepository.save(inventory);

        // Record initial stock when inventory is created with a positive quantity.
        if (savedInventory.getQuantity() > 0) {
            inventoryMovementService.record(
                    savedInventory,
                    InventoryMovementType.STOCK_ADDED,
                    0,
                    savedInventory.getQuantity(),
                    0,
                    0,
                    "INVENTORY",
                    savedInventory.getId(),
                    "Initial inventory stock created",
                    null
            );
        }

        return inventoryMapper.toResponse(savedInventory);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getById(int id) {

        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Inventory not found"));

        return inventoryMapper.toResponse(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getByProductId(int productId) {

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inventory not found for product"
                        ));

        return inventoryMapper.toResponse(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getAll() {

        return inventoryRepository.findAll()
                .stream()
                .map(inventoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public InventoryResponse updateStock(int id, int quantity) {

        if (quantity < 0) {
            throw new IllegalArgumentException(
                    "Total stock cannot be negative"
            );
        }

        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Inventory not found"));

        if (quantity < inventory.getReservedQuantity()) {
            throw new ConflictException(
                    "Total stock cannot be less than reserved stock"
            );
        }

        int quantityBefore = inventory.getQuantity();
        int reservedBefore = inventory.getReservedQuantity();

        inventory.setQuantity(quantity);

        Inventory updatedInventory = inventoryRepository.save(inventory);

        if (quantity != quantityBefore) {
            InventoryMovementType movementType;

            if (quantity > quantityBefore) {
                movementType = InventoryMovementType.STOCK_ADDED;
            } else if (quantity < quantityBefore) {
                movementType = InventoryMovementType.STOCK_REMOVED;
            } else {
                movementType = InventoryMovementType.STOCK_ADJUSTED;
            }

            inventoryMovementService.record(
                    updatedInventory,
                    movementType,
                    quantityBefore,
                    updatedInventory.getQuantity(),
                    reservedBefore,
                    updatedInventory.getReservedQuantity(),
                    "INVENTORY",
                    updatedInventory.getId(),
                    "Administrator updated total stock",
                    null
            );
        }

        return inventoryMapper.toResponse(updatedInventory);
    }

    @Override
    @Transactional
    public InventoryResponse addStock(int id, int quantity) {

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity to add must be greater than zero"
            );
        }

        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Inventory not found"));

        int quantityBefore = inventory.getQuantity();
        int reservedBefore = inventory.getReservedQuantity();

        inventory.setQuantity(
                Math.addExact(quantityBefore, quantity)
        );

        Inventory updatedInventory = inventoryRepository.save(inventory);

        inventoryMovementService.record(
                updatedInventory,
                InventoryMovementType.STOCK_ADDED,
                quantityBefore,
                updatedInventory.getQuantity(),
                reservedBefore,
                updatedInventory.getReservedQuantity(),
                "INVENTORY",
                updatedInventory.getId(),
                "Additional stock added",
                null
        );

        return inventoryMapper.toResponse(updatedInventory);
    }
}
