package com.ecommerce.category.controller;

import com.ecommerce.category.dto.CategoryCreateRequest;
import com.ecommerce.category.dto.CategoryResponse;
import com.ecommerce.category.service.CategoryService;
import com.ecommerce.common.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryCreateRequest request) {

        CategoryResponse response = categoryService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Category created successfully",
                        response
                ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(
            @PathVariable int id) {

        CategoryResponse response = categoryService.getById(id);

        return ResponseEntity.ok(ApiResponse.success(
                "Category retrieved successfully",
                response
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll() {

        List<CategoryResponse> response = categoryService.getAll();

        return ResponseEntity.ok(ApiResponse.success(
                "Categories retrieved successfully",
                response
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable int id,
            @Valid @RequestBody CategoryCreateRequest request) {

        CategoryResponse response =
                categoryService.update(id, request);

        return ResponseEntity.ok(ApiResponse.success(
                "Category updated successfully",
                response
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable int id) {

        categoryService.delete(id);

        return ResponseEntity.ok(
                ApiResponse.<Void>success(
                        "Category deleted successfully",
                        null
                )
        );
    }
}
