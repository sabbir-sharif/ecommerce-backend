package com.ecommerce.product.service;

import com.ecommerce.category.entity.Category;
import com.ecommerce.category.entity.CategoryStatus;
import com.ecommerce.category.repository.CategoryRepository;
import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.product.dto.ProductCreateRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.ProductUpdateRequest;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category category;
    private Product product;
    private ProductResponse response;

    @BeforeEach
    void setUp() {

        category = new Category();
        category.setId(1);
        category.setName("Electronics");
        category.setStatus(CategoryStatus.ACTIVE);

        product = new Product();
        product.setId(10);
        product.setName("Samsung Phone");
        product.setDescription("Android smartphone");
        product.setSku("SAM-001");
        product.setPrice(new BigDecimal("500.00"));
        product.setStatus(ProductStatus.ACTIVE);
        product.setCategory(category);

        response = new ProductResponse();
        response.setId(10);
        response.setName("Samsung Phone");
        response.setDescription("Android smartphone");
        response.setSku("SAM-001");
        response.setPrice(new BigDecimal("500.00"));
        response.setStatus(ProductStatus.ACTIVE);
        response.setCategoryId(1);
        response.setCategoryName("Electronics");
    }
    // Create test
    @Test
    void create_shouldCreateProductSuccessfully() {

        ProductCreateRequest request = new ProductCreateRequest();
        request.setName("Samsung Phone");
        request.setDescription("Android smartphone");
        request.setSku("SAM-001");
        request.setPrice(new BigDecimal("500.00"));
        request.setCategoryId(1);

        when(productRepository.existsBySku("SAM-001"))
                .thenReturn(false);

        when(categoryRepository.findById(1))
                .thenReturn(Optional.of(category));

        when(productMapper.toEntity(request))
                .thenReturn(product);

        when(productRepository.save(product))
                .thenReturn(product);

        when(productMapper.toResponse(product))
                .thenReturn(response);

        ProductResponse result = productService.create(request);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals("Samsung Phone", result.getName());
        assertEquals("SAM-001", result.getSku());
        assertEquals(ProductStatus.ACTIVE, result.getStatus());

        assertSame(category, product.getCategory());

        verify(productRepository).save(product);
        verify(productMapper).toResponse(product);
    }

    @Test
    void create_shouldThrowConflictWhenSkuAlreadyExists() {

        ProductCreateRequest request = new ProductCreateRequest();
        request.setSku("SAM-001");
        request.setCategoryId(1);

        when(productRepository.existsBySku("SAM-001"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> productService.create(request)
        );

        verify(categoryRepository, never()).findById(anyInt());
        verify(productRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowExceptionWhenCategoryNotFound() {

        ProductCreateRequest request = new ProductCreateRequest();
        request.setSku("SAM-001");
        request.setCategoryId(99);

        when(productRepository.existsBySku("SAM-001"))
                .thenReturn(false);

        when(categoryRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.create(request)
        );

        verify(productMapper, never()).toEntity(any());
        verify(productRepository, never()).save(any());
    }

    // GET BY ID TESTS
    @Test
    void getById_shouldReturnProductSuccessfully() {

        when(productRepository.findById(10))
                .thenReturn(Optional.of(product));

        when(productMapper.toResponse(product))
                .thenReturn(response);

        ProductResponse result = productService.getById(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals("Samsung Phone", result.getName());

        verify(productRepository).findById(10);
        verify(productMapper).toResponse(product);
    }

    @Test
    void getById_shouldThrowExceptionWhenProductNotFound() {

        when(productRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.getById(99)
        );

        verify(productMapper, never()).toResponse(any());
    }

    // GET ALL TESTS
    @Test
    void getAll_shouldReturnAllProducts() {

        Product secondProduct = new Product();
        secondProduct.setId(11);
        secondProduct.setName("Laptop");

        ProductResponse secondResponse = new ProductResponse();
        secondResponse.setId(11);
        secondResponse.setName("Laptop");

        when(productRepository.findAll())
                .thenReturn(List.of(product, secondProduct));

        when(productMapper.toResponse(product))
                .thenReturn(response);

        when(productMapper.toResponse(secondProduct))
                .thenReturn(secondResponse);

        List<ProductResponse> result = productService.getAll();

        assertEquals(2, result.size());
        assertEquals("Samsung Phone", result.get(0).getName());
        assertEquals("Laptop", result.get(1).getName());

        verify(productRepository).findAll();
        verify(productMapper, times(2)).toResponse(any(Product.class));
    }

    @Test
    void getAll_shouldReturnEmptyListWhenNoProductsExist() {

        when(productRepository.findAll())
                .thenReturn(List.of());

        List<ProductResponse> result = productService.getAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(productRepository).findAll();
        verifyNoInteractions(productMapper);
    }

    // UPDATE TESTS
    @Test
    void update_shouldUpdateProductSuccessfully() {

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("Updated Samsung Phone");
        request.setDescription("Updated description");
        request.setSku("SAM-002");
        request.setPrice(new BigDecimal("600.00"));
        request.setCategoryId(1);
        request.setStatus(ProductStatus.INACTIVE);

        when(productRepository.existsBySkuAndIdNot("SAM-002", 10))
                .thenReturn(false);

        when(productRepository.findById(10))
                .thenReturn(Optional.of(product));

        when(categoryRepository.findById(1))
                .thenReturn(Optional.of(category));

        when(productRepository.save(product))
                .thenReturn(product);

        when(productMapper.toResponse(product))
                .thenReturn(response);

        ProductResponse result = productService.update(10, request);

        assertNotNull(result);

        assertEquals("Updated Samsung Phone", product.getName());
        assertEquals("Updated description", product.getDescription());
        assertEquals("SAM-002", product.getSku());
        assertEquals(new BigDecimal("600.00"), product.getPrice());
        assertEquals(ProductStatus.INACTIVE, product.getStatus());
        assertSame(category, product.getCategory());

        verify(productRepository).save(product);
        verify(productMapper).toResponse(product);
    }

    @Test
    void update_shouldThrowConflictWhenAnotherProductHasSameSku() {

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setSku("EXISTING-SKU");

        when(productRepository.existsBySkuAndIdNot("EXISTING-SKU", 10))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> productService.update(10, request)
        );

        verify(productRepository, never()).findById(anyInt());
        verify(productRepository, never()).save(any());
    }

    @Test
    void update_shouldThrowExceptionWhenProductNotFound() {

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setSku("NEW-SKU");

        when(productRepository.existsBySkuAndIdNot("NEW-SKU", 99))
                .thenReturn(false);

        when(productRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.update(99, request)
        );

        verify(categoryRepository, never()).findById(anyInt());
        verify(productRepository, never()).save(any());
    }

    @Test
    void update_shouldThrowExceptionWhenCategoryNotFound() {

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setSku("NEW-SKU");
        request.setCategoryId(99);

        when(productRepository.existsBySkuAndIdNot("NEW-SKU", 10))
                .thenReturn(false);

        when(productRepository.findById(10))
                .thenReturn(Optional.of(product));

        when(categoryRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.update(10, request)
        );

        verify(productRepository, never()).save(any());
    }

    // DELETE TESTS
   @Test
    void delete_shouldDeleteProductSuccessfully() {

        when(productRepository.findById(10))
                .thenReturn(Optional.of(product));

        productService.delete(10);

        verify(productRepository).findById(10);
        verify(productRepository).delete(product);
    }

    @Test
    void delete_shouldThrowExceptionWhenProductNotFound() {

        when(productRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.delete(99)
        );

        verify(productRepository, never()).delete(any());
    }

    // SEARCH TESTS
    @Test
    void search_shouldReturnProductsSuccessfully() {

        Pageable pageable = PageRequest.of(
                0, 10, Sort.by(Sort.Direction.ASC, "name")
        );

        Page<Product> productPage =
                new PageImpl<>(List.of(product), pageable, 1);

        when(productRepository.searchProducts(
                eq("phone"),
                eq(1),
                eq(new BigDecimal("100.00")),
                eq(new BigDecimal("1000.00")),
                eq(ProductStatus.ACTIVE),
                any(Pageable.class)
        )).thenReturn(productPage);

        when(productMapper.toResponse(product))
                .thenReturn(response);

        Page<ProductResponse> result = productService.search(
                "phone",
                1,
                new BigDecimal("100.00"),
                new BigDecimal("1000.00"),
                0,
                10,
                "name",
                "ASC"
        );

        assertEquals(1, result.getTotalElements());
        assertEquals("Samsung Phone", result.getContent().get(0).getName());

        verify(productRepository).searchProducts(
                eq("phone"),
                eq(1),
                eq(new BigDecimal("100.00")),
                eq(new BigDecimal("1000.00")),
                eq(ProductStatus.ACTIVE),
                any(Pageable.class)
        );
    }

    @Test
    void search_shouldTrimKeyword() {

        when(productRepository.searchProducts(
                eq("phone"),
                isNull(),
                isNull(),
                isNull(),
                eq(ProductStatus.ACTIVE),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        productService.search(
                "  phone  ", null, null, null,
                0, 10, "name", "ASC"
        );

        verify(productRepository).searchProducts(
                eq("phone"),
                isNull(),
                isNull(),
                isNull(),
                eq(ProductStatus.ACTIVE),
                any(Pageable.class)
        );
    }

    @Test
    void search_shouldConvertBlankKeywordToNull() {

        when(productRepository.searchProducts(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(ProductStatus.ACTIVE),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        productService.search(
                "   ", null, null, null,
                0, 10, "name", "ASC"
        );

        verify(productRepository).searchProducts(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(ProductStatus.ACTIVE),
                any(Pageable.class)
        );
    }

    @Test
    void search_shouldRejectNegativePageNumber() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.search(
                        null, null, null, null,
                        -1, 10, "name", "ASC"
                )
        );

        verifyNoInteractions(productRepository);
    }

    @Test
    void search_shouldRejectZeroPageSize() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.search(
                        null, null, null, null,
                        0, 0, "name", "ASC"
                )
        );

        verifyNoInteractions(productRepository);
    }

    @Test
    void search_shouldRejectPageSizeGreaterThan100() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.search(
                        null, null, null, null,
                        0, 101, "name", "ASC"
                )
        );

        verifyNoInteractions(productRepository);
    }

    @Test
    void search_shouldRejectNegativeMinimumPrice() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.search(
                        null, null, new BigDecimal("-1.00"), null,
                        0, 10, "name", "ASC"
                )
        );

        verifyNoInteractions(productRepository);
    }

    @Test
    void search_shouldRejectNegativeMaximumPrice() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.search(
                        null, null, null, new BigDecimal("-1.00"),
                        0, 10, "name", "ASC"
                )
        );

        verifyNoInteractions(productRepository);
    }

    @Test
    void search_shouldRejectMinimumPriceGreaterThanMaximumPrice() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.search(
                        null, null,
                        new BigDecimal("1000.00"),
                        new BigDecimal("500.00"),
                        0, 10, "name", "ASC"
                )
        );

        verifyNoInteractions(productRepository);
    }

    @Test
    void search_shouldRejectNullSortField() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.search(
                        null, null, null, null,
                        0, 10, null, "ASC"
                )
        );

        verifyNoInteractions(productRepository);
    }

    @Test
    void search_shouldRejectUnsupportedSortField() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.search(
                        null, null, null, null,
                        0, 10, "categoryName", "ASC"
                )
        );

        verifyNoInteractions(productRepository);
    }

    @Test
    void search_shouldRejectNullSortDirection() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.search(
                        null, null, null, null,
                        0, 10, "name", null
                )
        );

        verifyNoInteractions(productRepository);
    }

    @Test
    void search_shouldRejectInvalidSortDirection() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.search(
                        null, null, null, null,
                        0, 10, "name", "SIDEWAYS"
                )
        );

        verifyNoInteractions(productRepository);
    }

    @Test
    void search_shouldAcceptMaximumPageSize() {

        when(productRepository.searchProducts(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(ProductStatus.ACTIVE),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        productService.search(
                null, null, null, null,
                0, 100, "price", "DESC"
        );

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(productRepository).searchProducts(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(ProductStatus.ACTIVE),
                captor.capture()
        );

        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(100, captor.getValue().getPageSize());
        assertEquals(
                Sort.Direction.DESC,
                captor.getValue().getSort().getOrderFor("price").getDirection()
        );
    }
}

