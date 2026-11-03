package com.ecommerce.category.service;

import com.ecommerce.category.dto.CategoryCreateRequest;
import com.ecommerce.category.dto.CategoryResponse;
import com.ecommerce.category.entity.Category;
import com.ecommerce.category.entity.CategoryStatus;
import com.ecommerce.category.mapper.CategoryMapper;
import com.ecommerce.category.repository.CategoryRepository;
import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.common.exception.ResourceNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category;
    private CategoryResponse response;

    @BeforeEach
    void setUp() {

        category = new Category();
        category.setId(1);
        category.setName("Electronics");
        category.setDescription("Electronic products");
        category.setStatus(CategoryStatus.ACTIVE);

        response = new CategoryResponse();
        response.setId(1);
        response.setName("Electronics");
        response.setDescription("Electronic products");
        response.setStatus(CategoryStatus.ACTIVE);
    }

    // CREATE TESTS
    @Test
    void create_shouldCreateCategorySuccessfully() {

        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("Electronics");
        request.setDescription("Electronic products");

        when(categoryRepository.existsByName("Electronics"))
                .thenReturn(false);

        when(categoryMapper.toEntity(request))
                .thenReturn(category);

        when(categoryRepository.save(category))
                .thenReturn(category);

        when(categoryMapper.toResponse(category))
                .thenReturn(response);

        CategoryResponse result = categoryService.create(request);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Electronics", result.getName());
        assertEquals(CategoryStatus.ACTIVE, result.getStatus());

        // New categories should always be ACTIVE.
        assertEquals(CategoryStatus.ACTIVE, category.getStatus());

        verify(categoryRepository).existsByName("Electronics");
        verify(categoryRepository).save(category);
        verify(categoryMapper).toResponse(category);
    }

    @Test
    void create_shouldThrowConflictWhenNameAlreadyExists() {

        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("Electronics");

        when(categoryRepository.existsByName("Electronics"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> categoryService.create(request)
        );

        verify(categoryMapper, never()).toEntity(any());
        verify(categoryRepository, never()).save(any());
    }

    // GET BY ID TESTS
    @Test
    void getById_shouldReturnCategorySuccessfully() {

        when(categoryRepository.findById(1))
                .thenReturn(Optional.of(category));

        when(categoryMapper.toResponse(category))
                .thenReturn(response);

        CategoryResponse result = categoryService.getById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Electronics", result.getName());

        verify(categoryRepository).findById(1);
        verify(categoryMapper).toResponse(category);
    }

    @Test
    void getById_shouldThrowExceptionWhenCategoryNotFound() {

        when(categoryRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> categoryService.getById(99)
        );

        verify(categoryMapper, never()).toResponse(any());
    }

    // GET ALL TESTS
    @Test
    void getAll_shouldReturnAllCategories() {

        Category secondCategory = new Category();
        secondCategory.setId(2);
        secondCategory.setName("Clothing");
        secondCategory.setStatus(CategoryStatus.ACTIVE);

        CategoryResponse secondResponse = new CategoryResponse();
        secondResponse.setId(2);
        secondResponse.setName("Clothing");

        when(categoryRepository.findAll())
                .thenReturn(List.of(category, secondCategory));

        when(categoryMapper.toResponse(category))
                .thenReturn(response);

        when(categoryMapper.toResponse(secondCategory))
                .thenReturn(secondResponse);

        List<CategoryResponse> result = categoryService.getAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Electronics", result.get(0).getName());
        assertEquals("Clothing", result.get(1).getName());

        verify(categoryRepository).findAll();
        verify(categoryMapper, times(2))
                .toResponse(any(Category.class));
    }

    @Test
    void getAll_shouldReturnEmptyListWhenNoCategoriesExist() {

        when(categoryRepository.findAll())
                .thenReturn(List.of());

        List<CategoryResponse> result = categoryService.getAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(categoryRepository).findAll();
        verifyNoInteractions(categoryMapper);
    }

    // UPDATE TESTS
    @Test
    void update_shouldUpdateCategorySuccessfully() {

        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("Home Appliances");
        request.setDescription("Home appliance products");

        CategoryResponse updatedResponse = new CategoryResponse();
        updatedResponse.setId(1);
        updatedResponse.setName("Home Appliances");
        updatedResponse.setDescription("Home appliance products");
        updatedResponse.setStatus(CategoryStatus.ACTIVE);

        when(categoryRepository.findById(1))
                .thenReturn(Optional.of(category));

        when(categoryRepository.existsByName("Home Appliances"))
                .thenReturn(false);

        when(categoryRepository.save(category))
                .thenReturn(category);

        when(categoryMapper.toResponse(category))
                .thenReturn(updatedResponse);

        CategoryResponse result = categoryService.update(1, request);

        assertNotNull(result);
        assertEquals("Home Appliances", result.getName());
        assertEquals(
                "Home appliance products",
                result.getDescription()
        );

        assertEquals("Home Appliances", category.getName());
        assertEquals(
                "Home appliance products",
                category.getDescription()
        );

        verify(categoryRepository).save(category);
        verify(categoryMapper).toResponse(category);
    }

    @Test
    void update_shouldNotCheckDuplicateWhenNameRemainsSame() {

        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("Electronics");
        request.setDescription("Updated electronic products");

        CategoryResponse updatedResponse = new CategoryResponse();
        updatedResponse.setId(1);
        updatedResponse.setName("Electronics");
        updatedResponse.setDescription("Updated electronic products");
        updatedResponse.setStatus(CategoryStatus.ACTIVE);

        when(categoryRepository.findById(1))
                .thenReturn(Optional.of(category));

        when(categoryRepository.save(category))
                .thenReturn(category);

        when(categoryMapper.toResponse(category))
                .thenReturn(updatedResponse);

        CategoryResponse result = categoryService.update(1, request);

        assertNotNull(result);
        assertEquals("Electronics", result.getName());
        assertEquals(
                "Updated electronic products",
                result.getDescription()
        );

        verify(categoryRepository, never())
                .existsByName(anyString());

        verify(categoryRepository).save(category);
    }

    @Test
    void update_shouldThrowConflictWhenAnotherCategoryHasSameName() {

        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("Clothing");

        when(categoryRepository.findById(1))
                .thenReturn(Optional.of(category));

        when(categoryRepository.existsByName("Clothing"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> categoryService.update(1, request)
        );

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_shouldThrowExceptionWhenCategoryNotFound() {

        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("New Category");

        when(categoryRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> categoryService.update(99, request)
        );

        verify(categoryRepository, never()).save(any());
        verify(categoryRepository, never()).existsByName(anyString());
    }

    // DELETE TESTS
    @Test
    void delete_shouldDeleteCategorySuccessfully() {

        when(categoryRepository.findById(1))
                .thenReturn(Optional.of(category));

        categoryService.delete(1);

        verify(categoryRepository).findById(1);
        verify(categoryRepository).delete(category);
    }

    @Test
    void delete_shouldThrowExceptionWhenCategoryNotFound() {

        when(categoryRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> categoryService.delete(99)
        );

        verify(categoryRepository, never()).delete(any());
    }
}

