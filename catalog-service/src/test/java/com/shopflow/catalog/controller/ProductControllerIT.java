package com.shopflow.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.catalog.PostgresOpenSearchTestBase;
import com.shopflow.catalog.document.ProductDocument;
import com.shopflow.catalog.dto.ProductRequest;
import com.shopflow.catalog.entity.Product;
import com.shopflow.catalog.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SuppressWarnings("null")
class ProductControllerIT extends PostgresOpenSearchTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/catalog/products";

    @BeforeEach
    void clearDatabase() {
        var indexOps = elasticsearchOperations.indexOps(ProductDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();
        }
        indexOps.createWithMapping();
        productRepository.deleteAll();
    }

    private Product saveProduct(String sku, boolean active) {
        return productRepository.save(Product.builder()
                .name("Test Product")
                .description("A test product")
                .price(new BigDecimal("29.99"))
                .stockQuantity(50)
                .sku(sku)
                .slug("test-product-" + sku.toLowerCase())
                .active(active)
                .build());
    }

    private ProductRequest buildRequest(String sku) {
        return ProductRequest.builder()
                .name("New Product")
                .description("A brand new product")
                .price(new BigDecimal("49.99"))
                .stockQuantity(100)
                .sku(sku)
                .slug("new-product-" + sku.toLowerCase())
                .active(true)
                .build();
    }

    @Test
    void getAllActiveProducts_returnsOkWithPagedResults() throws Exception {
        saveProduct("SKU-001", true);
        saveProduct("SKU-002", false);

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getProductById_whenExists_returnsOk() throws Exception {
        Product saved = saveProduct("SKU-001", true);

        mockMvc.perform(get(BASE_URL + "/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-001"));
    }

    @Test
    void getProductById_whenNotFound_returnsNotFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProduct_withValidRequest_returnsCreated() throws Exception {
        String body = objectMapper.writeValueAsString(buildRequest("SKU-NEW"));

        mockMvc.perform(post(BASE_URL)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("SKU-NEW"))
                .andExpect(jsonPath("$.price").value(49.99));
    }

    @Test
    void createProduct_withNoAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("SKU-NA"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProduct_withMissingFields_returnsBadRequest() throws Exception {
        String invalidBody = objectMapper.writeValueAsString(
                ProductRequest.builder().name("Incomplete").build());

        mockMvc.perform(post(BASE_URL)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void deleteProduct_withAuth_returnsNoContent() throws Exception {
        Product saved = saveProduct("SKU-DEL", true);

        mockMvc.perform(delete(BASE_URL + "/" + saved.getId())
                        .with(jwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateProduct_withValidRequest_returnsOk() throws Exception {
        Product saved = saveProduct("SKU-UPD", true);
        ProductRequest updated = buildRequest("SKU-UPD");
        updated.setName("Updated Product");
        updated.setSlug("updated-product-sku-upd");

        mockMvc.perform(put(BASE_URL + "/" + saved.getId())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Product"));
    }
}
