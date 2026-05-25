package com.shopflow.cart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.cart.RedisTestContainerBase;
import com.shopflow.cart.dto.AddItemRequest;
import com.shopflow.cart.dto.UpdateQuantityRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SuppressWarnings("null")
class CartControllerIT extends RedisTestContainerBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_USER_ID  = "test-user-123";
    private static final String TEST_GUEST_ID = "guest-abc-456";
    private static final String USER_URL       = "/api/v1/cart";
    private static final String GUEST_URL      = "/api/v1/cart/guest/" + TEST_GUEST_ID;

    @BeforeEach
    void flushRedis() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    private AddItemRequest buildAddRequest(Long productId) {
        return AddItemRequest.builder()
                .productId(productId)
                .productName("Widget " + productId)
                .sku("SKU-" + productId)
                .unitPrice(new BigDecimal("19.99"))
                .quantity(1)
                .build();
    }

    // ── Authenticated user cart ─────────────────────────────────────────────

    @Test
    void getUserCart_withValidJwt_returnsOk() throws Exception {
        mockMvc.perform(get(USER_URL)
                        .with(jwt().jwt(j -> j.subject(TEST_USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void getUserCart_withNoJwt_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(USER_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addItemToUserCart_withValidJwt_addsItemAndReturnsCart() throws Exception {
        String body = objectMapper.writeValueAsString(buildAddRequest(1L));

        mockMvc.perform(post(USER_URL + "/items")
                        .with(jwt().jwt(j -> j.subject(TEST_USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId").value(1));
    }

    @Test
    void addItemToUserCart_withMissingFields_returnsBadRequest() throws Exception {
        String invalidBody = objectMapper.writeValueAsString(
                AddItemRequest.builder().productId(1L).build());

        mockMvc.perform(post(USER_URL + "/items")
                        .with(jwt().jwt(j -> j.subject(TEST_USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void updateCartItemQuantity_updatesAndReturnsCart() throws Exception {
        // First add an item
        mockMvc.perform(post(USER_URL + "/items")
                .with(jwt().jwt(j -> j.subject(TEST_USER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildAddRequest(1L))));

        // Then update its quantity
        String updateBody = objectMapper.writeValueAsString(new UpdateQuantityRequest(5));
        mockMvc.perform(put(USER_URL + "/items/1")
                        .with(jwt().jwt(j -> j.subject(TEST_USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(5));
    }

    @Test
    void removeItemFromUserCart_removesItem() throws Exception {
        mockMvc.perform(post(USER_URL + "/items")
                .with(jwt().jwt(j -> j.subject(TEST_USER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildAddRequest(1L))));

        mockMvc.perform(delete(USER_URL + "/items/1")
                        .with(jwt().jwt(j -> j.subject(TEST_USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void clearUserCart_returnsNoContent() throws Exception {
        mockMvc.perform(delete(USER_URL)
                        .with(jwt().jwt(j -> j.subject(TEST_USER_ID))))
                .andExpect(status().isNoContent());
    }

    // ── Guest cart ──────────────────────────────────────────────────────────

    @Test
    void getGuestCart_withNoAuth_returnsOk() throws Exception {
        mockMvc.perform(get(GUEST_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestId").value(TEST_GUEST_ID));
    }

    @Test
    void addItemToGuestCart_withNoAuth_addsItem() throws Exception {
        String body = objectMapper.writeValueAsString(buildAddRequest(2L));

        mockMvc.perform(post(GUEST_URL + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void mergeGuestCart_withValidJwt_mergesAndReturnsUserCart() throws Exception {
        // Populate guest cart
        mockMvc.perform(post(GUEST_URL + "/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildAddRequest(5L))));

        // Merge into user cart
        mockMvc.perform(post(USER_URL + "/merge?guestId=" + TEST_GUEST_ID)
                        .with(jwt().jwt(j -> j.subject(TEST_USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId").value(5));
    }
}
