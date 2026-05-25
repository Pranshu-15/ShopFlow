package com.shopflow.catalog.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

@Document(indexName = "shopflow_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(name = "stock_quantity", type = FieldType.Integer)
    private Integer stockQuantity;

    @Field(type = FieldType.Keyword)
    private String sku;

    @Field(type = FieldType.Keyword)
    private String slug;

    @Field(name = "image_url", type = FieldType.Keyword)
    private String imageUrl;

    @Field(type = FieldType.Boolean)
    private boolean active;

    @Field(name = "category_id", type = FieldType.Long)
    private Long categoryId;

    @Field(name = "category_name", type = FieldType.Keyword)
    private String categoryName;
}
