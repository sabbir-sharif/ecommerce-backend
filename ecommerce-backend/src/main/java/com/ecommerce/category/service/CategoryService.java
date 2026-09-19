package com.ecommerce.category.service;

import com.ecommerce.category.dto.CategoryCreateRequest;
import com.ecommerce.category.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {

    CategoryResponse create(CategoryCreateRequest request);

    CategoryResponse getById(int id);

    List<CategoryResponse> getAll();

    CategoryResponse update(int id, CategoryCreateRequest request);

    void delete(int id);
}
