package com.shopflow.catalog.service;

import com.shopflow.catalog.document.ProductDocument;
import com.shopflow.catalog.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexedObjectInformation;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductIndexService {

    private static final String INDEX = "shopflow_products";

    private final ElasticsearchOperations elasticsearchOperations;

    public void indexProduct(Product product) {
        try {
            elasticsearchOperations.save(toDocument(product), IndexCoordinates.of(INDEX));
            log.debug("Indexed product {} to OpenSearch", product.getId());
        } catch (Exception e) {
            log.error("Failed to index product {} to OpenSearch: {}", product.getId(), e.getMessage());
        }
    }

    public void removeFromIndex(Long productId) {
        try {
            elasticsearchOperations.delete(String.valueOf(productId), IndexCoordinates.of(INDEX));
            log.debug("Removed product {} from OpenSearch index", productId);
        } catch (Exception e) {
            log.error("Failed to remove product {} from OpenSearch: {}", productId, e.getMessage());
        }
    }

    private ProductDocument toDocument(Product product) {
        return ProductDocument.builder()
                .id(String.valueOf(product.getId()))
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .sku(product.getSku())
                .slug(product.getSlug())
                .imageUrl(product.getImageUrl())
                .active(product.isActive())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .build();
    }
}
