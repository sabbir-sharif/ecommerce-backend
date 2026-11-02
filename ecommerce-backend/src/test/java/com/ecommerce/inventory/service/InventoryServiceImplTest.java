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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryMapper inventoryMapper;

    @Mock
    private InventoryMovementService inventoryMovementService;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1);

        inventory = new Inventory();
        inventory.setId(10);
        inventory.setProduct(product);
        inventory.setQuantity(10);
        inventory.setReservedQuantity(2);
    }

    // TEST 1: Successfully create inventory
    @Test
    void create_shouldCreateInventorySuccessfully() {

        InventoryCreateRequest request = new InventoryCreateRequest();
        request.setProductId(1);
        request.setQuantity(10);

        Inventory newInventory = new Inventory();
        newInventory.setQuantity(10);

        InventoryResponse expectedResponse = new InventoryResponse();
        expectedResponse.setId(10);
        expectedResponse.setProductId(1);
        expectedResponse.setQuantity(10);
        expectedResponse.setReservedQuantity(0);
        expectedResponse.setAvailableQuantity(10);

        when(inventoryRepository.existsByProductId(1))
                .thenReturn(false);

        when(productRepository.findById(1))
                .thenReturn(Optional.of(product));

        when(inventoryMapper.toEntity(request))
                .thenReturn(newInventory);

        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> {
                    Inventory saved = invocation.getArgument(0);
                    saved.setId(10);
                    return saved;
                });

        when(inventoryMapper.toResponse(any(Inventory.class)))
                .thenReturn(expectedResponse);

        InventoryResponse result = inventoryService.create(request);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals(10, result.getQuantity());
        assertEquals(0, result.getReservedQuantity());

        assertSame(product, newInventory.getProduct());

        verify(inventoryRepository).save(any(Inventory.class));

        verify(inventoryMovementService).record(
                any(Inventory.class),
                eq(InventoryMovementType.STOCK_ADDED),
                eq(0),
                eq(10),
                eq(0),
                eq(0),
                eq("INVENTORY"),
                eq(10),
                eq("Initial inventory stock created"),
                isNull()
        );
    }

    // TEST 2: Prevent duplicate inventory
    @Test
    void create_shouldThrowConflictWhenInventoryAlreadyExists() {

        InventoryCreateRequest request = new InventoryCreateRequest();
        request.setProductId(1);
        request.setQuantity(10);

        when(inventoryRepository.existsByProductId(1))
                .thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> inventoryService.create(request)
        );

        assertEquals(
                "Inventory already exists for this product",
                exception.getMessage()
        );

        verify(productRepository, never()).findById(anyInt());
        verify(inventoryRepository, never()).save(any());
    }

    // TEST 3: Product does not exist
    @Test
    void create_shouldThrowExceptionWhenProductNotFound() {

        InventoryCreateRequest request = new InventoryCreateRequest();
        request.setProductId(99);
        request.setQuantity(10);

        when(inventoryRepository.existsByProductId(99))
                .thenReturn(false);

        when(productRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> inventoryService.create(request)
        );

        verify(inventoryRepository, never()).save(any());
    }

    // TEST 4: Reject negative stock
    @Test
    void updateStock_shouldRejectNegativeQuantity() {

        assertThrows(
                IllegalArgumentException.class,
                () -> inventoryService.updateStock(10, -5)
        );

        verify(inventoryRepository, never()).findById(anyInt());
        verify(inventoryRepository, never()).save(any());
    }

    // TEST 5: Stock cannot become less than reserved quantity
    @Test
    void updateStock_shouldRejectQuantityBelowReservedStock() {

        when(inventoryRepository.findById(10))
                .thenReturn(Optional.of(inventory));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> inventoryService.updateStock(10, 1)
        );

        assertEquals(
                "Total stock cannot be less than reserved stock",
                exception.getMessage()
        );

        verify(inventoryRepository, never()).save(any());
    }

    // TEST 6: Successfully add stock
    @Test
    void addStock_shouldIncreaseQuantitySuccessfully() {

        InventoryResponse expectedResponse = new InventoryResponse();
        expectedResponse.setId(10);
        expectedResponse.setQuantity(15);
        expectedResponse.setReservedQuantity(2);
        expectedResponse.setAvailableQuantity(13);

        when(inventoryRepository.findById(10))
                .thenReturn(Optional.of(inventory));

        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(inventoryMapper.toResponse(inventory))
                .thenReturn(expectedResponse);

        InventoryResponse result = inventoryService.addStock(10, 5);

        assertEquals(15, inventory.getQuantity());
        assertEquals(15, result.getQuantity());
        assertEquals(13, result.getAvailableQuantity());

        verify(inventoryRepository).save(inventory);

        verify(inventoryMovementService).record(
                inventory,
                InventoryMovementType.STOCK_ADDED,
                10,
                15,
                2,
                2,
                "INVENTORY",
                10,
                "Additional stock added",
                null
        );
    }

    // TEST 7: Get inventory by ID successfully
    @Test
    void getById_shouldReturnInventorySuccessfully() {

        InventoryResponse expectedResponse = new InventoryResponse();
        expectedResponse.setId(10);
        expectedResponse.setQuantity(10);

        when(inventoryRepository.findById(10))
                .thenReturn(Optional.of(inventory));

        when(inventoryMapper.toResponse(inventory))
                .thenReturn(expectedResponse);

        InventoryResponse result = inventoryService.getById(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals(10, result.getQuantity());

        verify(inventoryRepository).findById(10);
        verify(inventoryMapper).toResponse(inventory);
    }

    // TEST 8: Get inventory by ID when it does not exist
    @Test
    void getById_shouldThrowExceptionWhenInventoryNotFound() {

        when(inventoryRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> inventoryService.getById(99)
        );

        verify(inventoryMapper, never()).toResponse(any());
    }

    // TEST 9: Get inventory by product ID successfully
    @Test
    void getByProductId_shouldReturnInventorySuccessfully() {

        InventoryResponse expectedResponse = new InventoryResponse();
        expectedResponse.setId(10);
        expectedResponse.setProductId(1);

        when(inventoryRepository.findByProductId(1))
                .thenReturn(Optional.of(inventory));

        when(inventoryMapper.toResponse(inventory))
                .thenReturn(expectedResponse);

        InventoryResponse result = inventoryService.getByProductId(1);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals(1, result.getProductId());

        verify(inventoryRepository).findByProductId(1);
    }

    // TEST 10: Get inventory by product ID when it does not exist
    @Test
    void getByProductId_shouldThrowExceptionWhenInventoryNotFound() {

        when(inventoryRepository.findByProductId(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> inventoryService.getByProductId(99)
        );

        verify(inventoryMapper, never()).toResponse(any());
    }

    // TEST 11: Get all inventories
    @Test
    void getAll_shouldReturnAllInventories() {

        Inventory secondInventory = new Inventory();
        secondInventory.setId(11);
        secondInventory.setQuantity(20);
        secondInventory.setReservedQuantity(5);

        InventoryResponse firstResponse = new InventoryResponse();
        firstResponse.setId(10);

        InventoryResponse secondResponse = new InventoryResponse();
        secondResponse.setId(11);

        when(inventoryRepository.findAll())
                .thenReturn(List.of(inventory, secondInventory));

        when(inventoryMapper.toResponse(inventory))
                .thenReturn(firstResponse);

        when(inventoryMapper.toResponse(secondInventory))
                .thenReturn(secondResponse);

        List<InventoryResponse> result = inventoryService.getAll();

        assertEquals(2, result.size());
        assertEquals(10, result.get(0).getId());
        assertEquals(11, result.get(1).getId());

        verify(inventoryRepository).findAll();
        verify(inventoryMapper, times(2)).toResponse(any(Inventory.class));
    }

    // TEST 12: Increase total stock and record STOCK_ADDED
    @Test
    void updateStock_shouldIncreaseQuantityAndRecordMovement() {

        InventoryResponse expectedResponse = new InventoryResponse();
        expectedResponse.setId(10);
        expectedResponse.setQuantity(15);
        expectedResponse.setReservedQuantity(2);
        expectedResponse.setAvailableQuantity(13);

        when(inventoryRepository.findById(10))
                .thenReturn(Optional.of(inventory));

        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(inventoryMapper.toResponse(inventory))
                .thenReturn(expectedResponse);

        InventoryResponse result = inventoryService.updateStock(10, 15);

        assertEquals(15, inventory.getQuantity());
        assertEquals(2, inventory.getReservedQuantity());
        assertEquals(13, result.getAvailableQuantity());

        verify(inventoryMovementService).record(
                inventory,
                InventoryMovementType.STOCK_ADDED,
                10,
                15,
                2,
                2,
                "INVENTORY",
                10,
                "Administrator updated total stock",
                null
        );
    }

    // TEST 13: Decrease total stock and record STOCK_REMOVED
    @Test
    void updateStock_shouldDecreaseQuantityAndRecordMovement() {

        InventoryResponse expectedResponse = new InventoryResponse();
        expectedResponse.setId(10);
        expectedResponse.setQuantity(8);
        expectedResponse.setReservedQuantity(2);
        expectedResponse.setAvailableQuantity(6);

        when(inventoryRepository.findById(10))
                .thenReturn(Optional.of(inventory));

        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(inventoryMapper.toResponse(inventory))
                .thenReturn(expectedResponse);

        InventoryResponse result = inventoryService.updateStock(10, 8);

        assertEquals(8, inventory.getQuantity());
        assertEquals(2, inventory.getReservedQuantity());
        assertEquals(6, result.getAvailableQuantity());

        verify(inventoryMovementService).record(
                inventory,
                InventoryMovementType.STOCK_REMOVED,
                10,
                8,
                2,
                2,
                "INVENTORY",
                10,
                "Administrator updated total stock",
                null
        );
    }

    // TEST 14: Do not record movement when quantity is unchanged
    @Test
    void updateStock_shouldNotRecordMovementWhenQuantityIsUnchanged() {

        InventoryResponse expectedResponse = new InventoryResponse();
        expectedResponse.setId(10);
        expectedResponse.setQuantity(10);

        when(inventoryRepository.findById(10))
                .thenReturn(Optional.of(inventory));

        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(inventoryMapper.toResponse(inventory))
                .thenReturn(expectedResponse);

        InventoryResponse result = inventoryService.updateStock(10, 10);

        assertEquals(10, result.getQuantity());

        verify(inventoryRepository).save(inventory);
        verifyNoInteractions(inventoryMovementService);
    }

    // TEST 15: Reject update when inventory does not exist
    @Test
    void updateStock_shouldThrowExceptionWhenInventoryNotFound() {

        when(inventoryRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> inventoryService.updateStock(99, 20)
        );

        verify(inventoryRepository, never()).save(any());
        verifyNoInteractions(inventoryMovementService);
    }

    // TEST 16: Reject zero quantity
    @Test
    void addStock_shouldRejectZeroQuantity() {

        assertThrows(
                IllegalArgumentException.class,
                () -> inventoryService.addStock(10, 0)
        );

        verify(inventoryRepository, never()).findById(anyInt());
        verify(inventoryRepository, never()).save(any());
        verifyNoInteractions(inventoryMovementService);
    }

    // TEST 17: Reject negative quantity
    @Test
    void addStock_shouldRejectNegativeQuantity() {

        assertThrows(
                IllegalArgumentException.class,
                () -> inventoryService.addStock(10, -5)
        );

        verify(inventoryRepository, never()).findById(anyInt());
        verify(inventoryRepository, never()).save(any());
        verifyNoInteractions(inventoryMovementService);
    }

    // TEST 18: Reject adding stock when inventory does not exist
    @Test
    void addStock_shouldThrowExceptionWhenInventoryNotFound() {

        when(inventoryRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> inventoryService.addStock(99, 5)
        );

        verify(inventoryRepository, never()).save(any());
        verifyNoInteractions(inventoryMovementService);
    }

    // TEST 19: Prevent integer overflow when adding stock
    @Test
    void addStock_shouldThrowExceptionWhenQuantityOverflows() {

        inventory.setQuantity(Integer.MAX_VALUE);

        when(inventoryRepository.findById(10))
                .thenReturn(Optional.of(inventory));

        assertThrows(
                ArithmeticException.class,
                () -> inventoryService.addStock(10, 1)
        );

        assertEquals(Integer.MAX_VALUE, inventory.getQuantity());

        verify(inventoryRepository, never()).save(any());
        verifyNoInteractions(inventoryMovementService);
    }

    // TEST 20: Create inventory with zero stock without recording movement
    @Test
    void create_shouldCreateZeroStockWithoutRecordingMovement() {

        InventoryCreateRequest request = new InventoryCreateRequest();
        request.setProductId(1);
        request.setQuantity(0);

        Inventory newInventory = new Inventory();
        newInventory.setQuantity(0);

        InventoryResponse expectedResponse = new InventoryResponse();
        expectedResponse.setId(10);
        expectedResponse.setProductId(1);
        expectedResponse.setQuantity(0);
        expectedResponse.setReservedQuantity(0);
        expectedResponse.setAvailableQuantity(0);

        when(inventoryRepository.existsByProductId(1))
                .thenReturn(false);

        when(productRepository.findById(1))
                .thenReturn(Optional.of(product));

        when(inventoryMapper.toEntity(request))
                .thenReturn(newInventory);

        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> {
                    Inventory saved = invocation.getArgument(0);
                    saved.setId(10);
                    return saved;
                });

        when(inventoryMapper.toResponse(newInventory))
                .thenReturn(expectedResponse);

        InventoryResponse result = inventoryService.create(request);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals(0, result.getQuantity());
        assertEquals(0, result.getReservedQuantity());
        assertEquals(0, result.getAvailableQuantity());

        assertSame(product, newInventory.getProduct());

        verify(inventoryRepository).save(newInventory);

        // Zero initial stock must not create a movement record.
        verifyNoInteractions(inventoryMovementService);
    }
}

