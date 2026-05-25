package com.shopflow.catalog.service;

import com.shopflow.catalog.dto.CategoryRequest;
import com.shopflow.catalog.dto.CategoryResponse;
import com.shopflow.catalog.entity.Category;
import com.shopflow.catalog.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category buildCategory(Long id, String name, String slug) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setSlug(slug);
        category.setDescription("Test category");
        return category;
    }

    private CategoryRequest buildRequest(String name, String slug, Long parentId) {
        return CategoryRequest.builder()
                .name(name)
                .slug(slug)
                .description("Test description")
                .parentId(parentId)
                .build();
    }

    @Test
    void getAllCategories_returnsMappedList() {
        when(categoryRepository.findAll())
                .thenReturn(List.of(buildCategory(1L, "Electronics", "electronics")));

        List<CategoryResponse> result = categoryService.getAllCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Electronics");
    }

    @Test
    void getCategoryById_whenExists_returnsResponse() {
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(buildCategory(1L, "Electronics", "electronics")));

        CategoryResponse response = categoryService.getCategoryById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getSlug()).isEqualTo("electronics");
    }

    @Test
    void getCategoryById_whenNotFound_throwsNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createCategory_withValidRequest_savesAndReturns() {
        when(categoryRepository.existsBySlug("electronics")).thenReturn(false);
        when(categoryRepository.existsByName("Electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(inv -> {
                    Category c = inv.getArgument(0);
                    c.setId(1L);
                    return c;
                });

        CategoryResponse response = categoryService.createCategory(buildRequest("Electronics", "electronics", null));

        assertThat(response.getName()).isEqualTo("Electronics");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_whenSlugExists_throwsConflict() {
        when(categoryRepository.existsBySlug("electronics")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(buildRequest("Electronics", "electronics", null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void deleteCategory_whenFound_deletesSuccessfully() {
        when(categoryRepository.existsById(1L)).thenReturn(true);

        categoryService.deleteCategory(1L);

        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void deleteCategory_whenNotFound_throwsNotFoundException() {
        when(categoryRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.deleteCategory(99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
