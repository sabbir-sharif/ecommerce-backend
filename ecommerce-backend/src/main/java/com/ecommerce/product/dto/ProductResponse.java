package com.ecommerce.product.dto;

import com.ecommerce.product.entity.ProductStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ProductResponse {

    private int id;

    private String name;

    private String description;

    private String sku;

    private BigDecimal price;

    private ProductStatus status;

    private int categoryId;

    private String categoryName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
