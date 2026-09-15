package com.ecommerce.category.dto;

import com.ecommerce.category.entity.CategoryStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CategoryResponse {

    private Long id;

    private String name;

    private String description;

    private CategoryStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
