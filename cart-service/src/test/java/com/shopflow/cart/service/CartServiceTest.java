package com.shopflow.cart.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.cart.dto.AddItemRequest;
import com.shopflow.cart.dto.UpdateQuantityRequest;
import com.shopflow.cart.model.CartItem;
import com.shopflow.cart.model.ShoppingCart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CartServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private CartService cartService;
    private ObjectMapper objectMapper;

    private static final String TEST_USER_ID  = "test-user-123";
    private static final String TEST_GUEST_ID = "guest-abc-456";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        cartService = new CartService(redisTemplate, objectMapper);
    }

    private AddItemRequest buildAddRequest(Long productId, int quantity) {
        return AddItemRequest.builder()
                .productId(productId)
                .productName("Widget " + productId)
                .sku("SKU-" + productId)
                .unitPrice(new BigDecimal("9.99"))
                .quantity(quantity)
                .build();
    }

    @Test
    void getUserCart_whenNoCartExists_returnsEmptyCart() {
        when(valueOps.get(anyString())).thenReturn(null);

        ShoppingCart cart = cartService.getUserCart(TEST_USER_ID);

        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getUserId()).isEqualTo(TEST_USER_ID);
        verify(valueOps).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void addItemToUserCart_newItem_addsToCart() {
        when(valueOps.get(anyString())).thenReturn(null);

        ShoppingCart cart = cartService.addItemToUserCart(TEST_USER_ID, buildAddRequest(1L, 2));

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(cart.getTotalPrice()).isEqualByComparingTo("19.98");
    }

    @Test
    void addItemToUserCart_existingItem_incrementsQuantity() throws Exception {
        ShoppingCart existing = ShoppingCart.builder()
                .userId(TEST_USER_ID)
                .build();
        existing.getItems().add(CartItem.builder()
                .productId(1L)
                .productName("Widget 1")
                .sku("SKU-1")
                .unitPrice(new BigDecimal("9.99"))
                .quantity(1)
                .build());

        when(valueOps.get(anyString())).thenReturn(objectMapper.writeValueAsString(existing));

        ShoppingCart cart = cartService.addItemToUserCart(TEST_USER_ID, buildAddRequest(1L, 3));

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(4);
    }

    @Test
    void removeItemFromUserCart_removesCorrectItem() throws Exception {
        ShoppingCart existing = ShoppingCart.builder().userId(TEST_USER_ID).build();
        existing.getItems().add(CartItem.builder().productId(1L).sku("SKU-1")
                .unitPrice(BigDecimal.TEN).quantity(1).build());
        existing.getItems().add(CartItem.builder().productId(2L).sku("SKU-2")
                .unitPrice(BigDecimal.TEN).quantity(1).build());

        when(valueOps.get(anyString())).thenReturn(objectMapper.writeValueAsString(existing));

        ShoppingCart cart = cartService.removeItemFromUserCart(TEST_USER_ID, 1L);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getProductId()).isEqualTo(2L);
    }

    @Test
    void updateUserCartItemQuantity_setsNewQuantity() throws Exception {
        ShoppingCart existing = ShoppingCart.builder().userId(TEST_USER_ID).build();
        existing.getItems().add(CartItem.builder().productId(1L).sku("SKU-1")
                .unitPrice(new BigDecimal("5.00")).quantity(2).build());

        when(valueOps.get(anyString())).thenReturn(objectMapper.writeValueAsString(existing));

        ShoppingCart cart = cartService.updateUserCartItemQuantity(
                TEST_USER_ID, 1L, new UpdateQuantityRequest(10));

        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(10);
    }

    @Test
    void updateUserCartItemQuantity_withZero_removesItem() throws Exception {
        ShoppingCart existing = ShoppingCart.builder().userId(TEST_USER_ID).build();
        existing.getItems().add(CartItem.builder().productId(1L).sku("SKU-1")
                .unitPrice(BigDecimal.TEN).quantity(1).build());

        when(valueOps.get(anyString())).thenReturn(objectMapper.writeValueAsString(existing));

        ShoppingCart cart = cartService.updateUserCartItemQuantity(
                TEST_USER_ID, 1L, new UpdateQuantityRequest(0));

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void mergeGuestCartIntoUserCart_combinedItemsInUserCart() throws Exception {
        ShoppingCart userCart = ShoppingCart.builder().userId(TEST_USER_ID).build();
        userCart.getItems().add(CartItem.builder().productId(1L).sku("SKU-1")
                .unitPrice(BigDecimal.TEN).quantity(1).build());

        ShoppingCart guestCart = ShoppingCart.builder().guestId(TEST_GUEST_ID).build();
        guestCart.getItems().add(CartItem.builder().productId(1L).sku("SKU-1")
                .unitPrice(BigDecimal.TEN).quantity(2).build());
        guestCart.getItems().add(CartItem.builder().productId(2L).sku("SKU-2")
                .unitPrice(BigDecimal.TEN).quantity(1).build());

        String userJson  = objectMapper.writeValueAsString(userCart);
        String guestJson = objectMapper.writeValueAsString(guestCart);

        when(valueOps.get("shopflow:cart:user:" + TEST_USER_ID)).thenReturn(userJson);
        when(valueOps.get("shopflow:cart:guest:" + TEST_GUEST_ID)).thenReturn(guestJson);

        ShoppingCart merged = cartService.mergeGuestCartIntoUserCart(TEST_USER_ID, TEST_GUEST_ID);

        assertThat(merged.getItems()).hasSize(2);
        assertThat(merged.getItems().stream()
                .filter(i -> i.getProductId().equals(1L))
                .findFirst().get().getQuantity()).isEqualTo(3);
        verify(redisTemplate).delete("shopflow:cart:guest:" + TEST_GUEST_ID);
    }

    @Test
    void clearUserCart_deletesRedisKey() {
        cartService.clearUserCart(TEST_USER_ID);
        verify(redisTemplate).delete("shopflow:cart:user:" + TEST_USER_ID);
    }

    @Test
    void getGuestCart_returnsCartWithGuestId() {
        when(valueOps.get(anyString())).thenReturn(null);

        ShoppingCart cart = cartService.getGuestCart(TEST_GUEST_ID);

        assertThat(cart.getGuestId()).isEqualTo(TEST_GUEST_ID);
        assertThat(cart.getUserId()).isNull();
    }
}
