package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductCreateRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.ProductUpdateRequest;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    ProductResponse create(ProductCreateRequest request);

    ProductResponse getById(int id);

    List<ProductResponse> getAll();

    ProductResponse update(int id, ProductUpdateRequest request);

    void delete(int id); // later active/inactive. product is related to other entity

    Page<ProductResponse> search(
            String keyword,
            Integer categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String direction
    );
}
