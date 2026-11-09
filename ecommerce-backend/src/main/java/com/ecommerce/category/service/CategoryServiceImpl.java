package com.ecommerce.category.service;

import com.ecommerce.category.dto.CategoryCreateRequest;
import com.ecommerce.category.dto.CategoryResponse;
import com.ecommerce.category.entity.Category;
import com.ecommerce.category.entity.CategoryStatus;
import com.ecommerce.category.mapper.CategoryMapper;
import com.ecommerce.category.repository.CategoryRepository;
import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @CacheEvict(
            cacheNames = {
                    "categoriesById",
                    "categoriesAll",
                    "productsById"
            },
            allEntries = true
    )
    public CategoryResponse create(CategoryCreateRequest request) {

        if (categoryRepository.existsByName(request.getName())) {
            throw new ConflictException("Category name already exists");
        }

        Category category = categoryMapper.toEntity(request);

        category.setStatus(CategoryStatus.ACTIVE);

        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    @Cacheable(
            cacheNames = "categoriesById",
            key = "#id"
    )
    public CategoryResponse getById(int id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found"));

        return categoryMapper.toResponse(category);
    }

    @Override
    @Cacheable(
            cacheNames = "categoriesAll",
            key = "'all'"
    )
    public List<CategoryResponse> getAll() {

        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    @CacheEvict(
            cacheNames = {
                    "categoriesById",
                    "categoriesAll",
                    "productsById"
            },
            allEntries = true
    )
    public CategoryResponse update(
            int id,
            CategoryCreateRequest request) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found"));


        // * Check whether another category already
        // * uses the requested name.

        if (!category.getName().equals(request.getName())
                && categoryRepository.existsByName(request.getName())) {

            throw new ConflictException("Category name already exists");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        Category updatedCategory =
                categoryRepository.save(category);

        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    @CacheEvict(
            cacheNames = {
                    "categoriesById",
                    "categoriesAll",
                    "productsById"
            },
            allEntries = true
    )
    public void delete(int id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found"));

        categoryRepository.delete(category);
    }
}
