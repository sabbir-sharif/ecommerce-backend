package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductCreateRequest;
import com.ecommerce.product.dto.ProductResponse;

import java.util.List;

public interface ProductService {

    ProductResponse create(ProductCreateRequest request);

    ProductResponse getById(int id);

    List<ProductResponse> getAll();

    ProductResponse update(int id, ProductCreateRequest request);

    void delete(int id); // later active/inactive. product is related to other entity
}
