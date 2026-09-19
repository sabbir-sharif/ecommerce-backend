package com.ecommerce.category.service;

import com.ecommerce.category.dto.CategoryCreateRequest;
import com.ecommerce.category.dto.CategoryResponse;
import com.ecommerce.category.entity.Category;
import com.ecommerce.category.entity.CategoryStatus;
import com.ecommerce.category.mapper.CategoryMapper;
import com.ecommerce.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public CategoryResponse create(CategoryCreateRequest request) {

        if (categoryRepository.existsByName(request.getName())) {
            throw new RuntimeException("Category name already exists");
        }

        Category category = categoryMapper.toEntity(request);

        category.setStatus(CategoryStatus.ACTIVE);

        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    public CategoryResponse getById(int id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Category not found"));

        return categoryMapper.toResponse(category);
    }

    @Override
    public List<CategoryResponse> getAll() {

        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse update(
            int id,
            CategoryCreateRequest request) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Category not found"));


        // * Check whether another category already
        // * uses the requested name.

        if (!category.getName().equals(request.getName())
                && categoryRepository.existsByName(request.getName())) {

            throw new RuntimeException("Category name already exists");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        Category updatedCategory =
                categoryRepository.save(category);

        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    public void delete(int id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Category not found"));

        categoryRepository.delete(category);
    }
}
