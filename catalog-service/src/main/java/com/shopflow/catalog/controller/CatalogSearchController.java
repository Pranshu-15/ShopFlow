package com.shopflow.catalog.controller;

import com.shopflow.catalog.dto.ProductResponse;
import com.shopflow.catalog.service.CatalogSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/catalog/search")
@RequiredArgsConstructor
public class CatalogSearchController {

    private final CatalogSearchService catalogSearchService;

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(catalogSearchService.search(q, categoryId, minPrice, maxPrice, pageable));
    }
}
