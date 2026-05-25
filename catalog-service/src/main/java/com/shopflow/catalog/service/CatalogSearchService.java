package com.shopflow.catalog.service;

import com.shopflow.catalog.document.ProductDocument;
import com.shopflow.catalog.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public Page<ProductResponse> search(
            String query,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {

        String jsonQuery = buildQuery(query, categoryId, minPrice, maxPrice);
        StringQuery stringQuery = new StringQuery(jsonQuery, pageable);

        SearchHits<ProductDocument> hits = elasticsearchOperations.search(stringQuery, ProductDocument.class);

        List<ProductResponse> results = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::documentToResponse)
                .toList();

        return new PageImpl<>(results, pageable, hits.getTotalHits());
    }

    private String buildQuery(String query, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice) {
        StringBuilder filters = new StringBuilder();

        filters.append("""
                {"term": {"active": true}}
                """);

        if (categoryId != null) {
            filters.append(",{\"term\": {\"category_id\": %d}}".formatted(categoryId));
        }

        if (minPrice != null || maxPrice != null) {
            StringBuilder range = new StringBuilder("""
                    ,{"range": {"price": {""");
            if (minPrice != null) range.append("""
                    "gte": %s""".formatted(minPrice.toPlainString()));
            if (minPrice != null && maxPrice != null) range.append(",");
            if (maxPrice != null) range.append("""
                    "lte": %s""".formatted(maxPrice.toPlainString()));
            range.append("}}}");
            filters.append(range);
        }

        if (query == null || query.isBlank()) {
            return """
                    {"bool": {"filter": [%s]}}
                    """.formatted(filters);
        }

        return """
                {"bool": {
                    "must": [{"multi_match": {"query": "%s", "fields": ["name^2", "description"]}}],
                    "filter": [%s]
                }}
                """.formatted(query.replace("\"", "\\\""), filters);
    }

    private ProductResponse documentToResponse(ProductDocument doc) {
        return ProductResponse.builder()
                .id(doc.getId() != null ? Long.parseLong(doc.getId()) : null)
                .name(doc.getName())
                .description(doc.getDescription())
                .price(doc.getPrice())
                .stockQuantity(doc.getStockQuantity())
                .sku(doc.getSku())
                .slug(doc.getSlug())
                .imageUrl(doc.getImageUrl())
                .active(doc.isActive())
                .categoryId(doc.getCategoryId())
                .categoryName(doc.getCategoryName())
                .build();
    }
}
