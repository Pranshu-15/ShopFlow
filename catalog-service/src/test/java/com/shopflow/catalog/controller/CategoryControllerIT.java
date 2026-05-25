package com.shopflow.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.catalog.PostgresOpenSearchTestBase;
import com.shopflow.catalog.dto.CategoryRequest;
import com.shopflow.catalog.entity.Category;
import com.shopflow.catalog.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SuppressWarnings("null")
class CategoryControllerIT extends PostgresOpenSearchTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/catalog/categories";

    @BeforeEach
    void clearDatabase() {
        categoryRepository.deleteAll();
    }

    private Category saveCategory(String name, String slug) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        category.setDescription("Test category");
        return categoryRepository.save(category);
    }

    private CategoryRequest buildRequest(String name, String slug) {
        return CategoryRequest.builder()
                .name(name)
                .slug(slug)
                .description("Test description")
                .build();
    }

    @Test
    void getAllCategories_returnsOk() throws Exception {
        saveCategory("Electronics", "electronics");

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getCategoryById_whenExists_returnsOk() throws Exception {
        Category saved = saveCategory("Electronics", "electronics");

        mockMvc.perform(get(BASE_URL + "/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    void getCategoryById_whenNotFound_returnsNotFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCategory_withValidRequest_returnsCreated() throws Exception {
        String body = objectMapper.writeValueAsString(buildRequest("Electronics", "electronics"));

        mockMvc.perform(post(BASE_URL)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.slug").value("electronics"));
    }

    @Test
    void createCategory_withNoAuth_returnsUnauthorized() throws Exception {
        String body = objectMapper.writeValueAsString(buildRequest("Electronics", "electronics"));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCategory_withDuplicateSlug_returnsConflict() throws Exception {
        saveCategory("Electronics", "electronics");
        String body = objectMapper.writeValueAsString(buildRequest("Electro", "electronics"));

        mockMvc.perform(post(BASE_URL)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteCategory_withAuth_returnsNoContent() throws Exception {
        Category saved = saveCategory("Electronics", "electronics");

        mockMvc.perform(delete(BASE_URL + "/" + saved.getId())
                        .with(jwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateCategory_withValidRequest_returnsOk() throws Exception {
        Category saved = saveCategory("Electronics", "electronics");
        String body = objectMapper.writeValueAsString(
                buildRequest("Consumer Electronics", "consumer-electronics"));

        mockMvc.perform(put(BASE_URL + "/" + saved.getId())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Consumer Electronics"));
    }
}
