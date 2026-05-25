package com.shopflow.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    private String name;

    private String description;

    @NotBlank(message = "Slug is required")
    private String slug;

    private Long parentId;
}
