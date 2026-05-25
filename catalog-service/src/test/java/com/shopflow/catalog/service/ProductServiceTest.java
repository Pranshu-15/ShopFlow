package com.shopflow.catalog.service;

import com.shopflow.catalog.dto.ProductRequest;
import com.shopflow.catalog.dto.ProductResponse;
import com.shopflow.catalog.entity.Category;
import com.shopflow.catalog.entity.Product;
import com.shopflow.catalog.repository.CategoryRepository;
import com.shopflow.catalog.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductIndexService productIndexService;

    @InjectMocks
    private ProductService productService;

    private Product buildProduct(Long id, String sku) {
        return Product.builder()
                .id(id)
                .name("Test Product")
                .description("A test product")
                .price(new BigDecimal("29.99"))
                .stockQuantity(100)
                .sku(sku)
                .slug("test-product-" + sku)
                .active(true)
                .build();
    }

    private ProductRequest buildRequest(String sku) {
        return ProductRequest.builder()
                .name("Test Product")
                .description("A test product")
                .price(new BigDecimal("29.99"))
                .stockQuantity(100)
                .sku(sku)
                .slug("test-product-" + sku)
                .active(true)
                .build();
    }

    @Test
    void getProductById_whenExists_returnsResponse() {
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(buildProduct(1L, "SKU-001")));

        ProductResponse response = productService.getProductById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getSku()).isEqualTo("SKU-001");
    }

    @Test
    void getProductById_whenNotFound_throwsNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createProduct_withValidRequest_savesAndIndexes() {
        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(productRepository.existsBySlug("test-product-SKU-001")).thenReturn(false);
        when(productRepository.save(any(Product.class)))
                .thenAnswer(inv -> {
                    Product p = inv.getArgument(0);
                    p.setId(1L);
                    return p;
                });

        ProductResponse response = productService.createProduct(buildRequest("SKU-001"));

        assertThat(response.getSku()).isEqualTo("SKU-001");
        verify(productIndexService).indexProduct(any(Product.class));
    }

    @Test
    void createProduct_whenSkuExists_throwsConflict() {
        when(productRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(buildRequest("SKU-001")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(productIndexService, never()).indexProduct(any());
    }

    @Test
    void updateProduct_whenFound_updatesAndReindexes() {
        Product existing = buildProduct(1L, "SKU-001");
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(productRepository.existsBySlug("test-product-SKU-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductRequest updated = buildRequest("SKU-001");
        updated.setName("Updated Product");

        ProductResponse response = productService.updateProduct(1L, updated);

        assertThat(response.getName()).isEqualTo("Updated Product");
        verify(productIndexService).indexProduct(any(Product.class));
    }

    @Test
    void deleteProduct_whenFound_deletesAndRemovesFromIndex() {
        Product product = buildProduct(1L, "SKU-001");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        verify(productRepository).delete(product);
        verify(productIndexService).removeFromIndex(1L);
    }

    @Test
    void deleteProduct_whenNotFound_throwsNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createProduct_withCategory_resolvesAndSetsCategory() {
        Category category = new Category();
        category.setId(5L);
        category.setName("Electronics");

        when(productRepository.existsBySku("SKU-002")).thenReturn(false);
        when(productRepository.existsBySlug("test-product-SKU-002")).thenReturn(false);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(2L);
            return p;
        });

        ProductRequest request = buildRequest("SKU-002");
        request.setCategoryId(5L);

        ProductResponse response = productService.createProduct(request);

        assertThat(response.getCategoryId()).isEqualTo(5L);
        assertThat(response.getCategoryName()).isEqualTo("Electronics");
    }
}
