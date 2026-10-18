package com.ecommerce.product.controller;

import com.ecommerce.common.response.ApiResponse;
import com.ecommerce.product.dto.ProductCreateRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody ProductCreateRequest request) {

        ProductResponse response = productService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Product created successfully",
                        response
                ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(
            @PathVariable int id) {

        ProductResponse response = productService.getById(id);

        return ResponseEntity.ok(ApiResponse.success(
                "Product retrieved successfully",
                response
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {

        Page<ProductResponse> result = productService.search(
                keyword,
                categoryId,
                minPrice,
                maxPrice,
                page,
                size,
                sortBy,
                direction
        );

        Map<String, Object> body = Map.of(
                "products", result.getContent(),
                "currentPage", result.getNumber(),
                "pageSize", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "isFirst", result.isFirst(),
                "isLast", result.isLast()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Products retrieved successfully",
                        body
                )
        );
    }

//    @GetMapping
//    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAll() {
//
//        List<ProductResponse> response = productService.getAll();
//
//        return ResponseEntity.ok(ApiResponse.success(
//                "Products retrieved successfully",
//                response
//        ));
//    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable int id,
            @Valid @RequestBody ProductCreateRequest request) {

        ProductResponse response =
                productService.update(id, request);

        return ResponseEntity.ok(ApiResponse.success(
                "Product updated successfully",
                response
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable int id) {

        productService.delete(id);

        return ResponseEntity.ok(
                ApiResponse.<Void>success(
                        "Product deleted successfully",
                        null
                )
        );
    }
}
