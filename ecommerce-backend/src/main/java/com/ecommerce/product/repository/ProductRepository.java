package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends
        JpaRepository<Product, Integer> {

    boolean existsBySku(@NotBlank @Size(max = 100) String sku);
}
