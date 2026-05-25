package com.shopflow.cart.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.cart.dto.AddItemRequest;
import com.shopflow.cart.dto.UpdateQuantityRequest;
import com.shopflow.cart.model.CartItem;
import com.shopflow.cart.model.ShoppingCart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private static final String USER_KEY_FORMAT  = "shopflow:cart:user:%s";
    private static final String GUEST_KEY_FORMAT = "shopflow:cart:guest:%s";
    private static final long   USER_CART_TTL    = 30L * 24 * 3600;
    private static final long   GUEST_CART_TTL   = 7L  * 24 * 3600;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // ── User cart operations ────────────────────────────────────────────────

    public ShoppingCart getUserCart(String userId) {
        return getOrCreate(userKey(userId), userId, null);
    }

    public ShoppingCart addItemToUserCart(String userId, AddItemRequest req) {
        return addItem(userKey(userId), userId, null, req, USER_CART_TTL);
    }

    public ShoppingCart updateUserCartItemQuantity(String userId, Long productId, UpdateQuantityRequest req) {
        return updateQuantity(userKey(userId), productId, req.getQuantity(), USER_CART_TTL);
    }

    public ShoppingCart removeItemFromUserCart(String userId, Long productId) {
        return removeItem(userKey(userId), productId, USER_CART_TTL);
    }

    public void clearUserCart(String userId) {
        redisTemplate.delete(userKey(userId));
    }

    // ── Guest cart operations ───────────────────────────────────────────────

    public ShoppingCart getGuestCart(String guestId) {
        return getOrCreate(guestKey(guestId), null, guestId);
    }

    public ShoppingCart addItemToGuestCart(String guestId, AddItemRequest req) {
        return addItem(guestKey(guestId), null, guestId, req, GUEST_CART_TTL);
    }

    public ShoppingCart updateGuestCartItemQuantity(String guestId, Long productId, UpdateQuantityRequest req) {
        return updateQuantity(guestKey(guestId), productId, req.getQuantity(), GUEST_CART_TTL);
    }

    public ShoppingCart removeItemFromGuestCart(String guestId, Long productId) {
        return removeItem(guestKey(guestId), productId, GUEST_CART_TTL);
    }

    public void clearGuestCart(String guestId) {
        redisTemplate.delete(guestKey(guestId));
    }

    // ── Merge ───────────────────────────────────────────────────────────────

    public ShoppingCart mergeGuestCartIntoUserCart(String userId, String guestId) {
        ShoppingCart userCart  = getOrCreate(userKey(userId), userId, null);
        ShoppingCart guestCart = getOrCreate(guestKey(guestId), null, guestId);

        for (CartItem guestItem : guestCart.getItems()) {
            userCart.getItems().stream()
                    .filter(i -> i.getProductId().equals(guestItem.getProductId()))
                    .findFirst()
                    .ifPresentOrElse(
                            existing -> existing.setQuantity(existing.getQuantity() + guestItem.getQuantity()),
                            () -> userCart.getItems().add(guestItem));
        }

        userCart.setUpdatedAt(Instant.now());
        save(userKey(userId), userCart, USER_CART_TTL);
        redisTemplate.delete(guestKey(guestId));
        return userCart;
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    private ShoppingCart getOrCreate(String key, String userId, String guestId) {
        String json = redisTemplate.opsForValue().get(key);
        if (json != null) {
            return deserialize(json);
        }
        ShoppingCart cart = ShoppingCart.builder()
                .userId(userId)
                .guestId(guestId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        save(key, cart, userId != null ? USER_CART_TTL : GUEST_CART_TTL);
        return cart;
    }

    private ShoppingCart addItem(String key, String userId, String guestId, AddItemRequest req, long ttl) {
        ShoppingCart cart = getOrCreate(key, userId, guestId);

        cart.getItems().stream()
                .filter(i -> i.getProductId().equals(req.getProductId()))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.setQuantity(existing.getQuantity() + req.getQuantity()),
                        () -> cart.getItems().add(CartItem.builder()
                                .productId(req.getProductId())
                                .productName(req.getProductName())
                                .sku(req.getSku())
                                .unitPrice(req.getUnitPrice())
                                .quantity(req.getQuantity())
                                .imageUrl(req.getImageUrl())
                                .build()));

        cart.setUpdatedAt(Instant.now());
        save(key, cart, ttl);
        return cart;
    }

    private ShoppingCart updateQuantity(String key, Long productId, int quantity, long ttl) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found");
        }
        ShoppingCart cart = deserialize(json);

        if (quantity <= 0) {
            cart.getItems().removeIf(i -> i.getProductId().equals(productId));
        } else {
            cart.getItems().stream()
                    .filter(i -> i.getProductId().equals(productId))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not in cart"))
                    .setQuantity(quantity);
        }

        cart.setUpdatedAt(Instant.now());
        save(key, cart, ttl);
        return cart;
    }

    private ShoppingCart removeItem(String key, Long productId, long ttl) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found");
        }
        ShoppingCart cart = deserialize(json);
        cart.getItems().removeIf(i -> i.getProductId().equals(productId));
        cart.setUpdatedAt(Instant.now());
        save(key, cart, ttl);
        return cart;
    }

    private void save(String key, ShoppingCart cart, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(cart), ttlSeconds, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to persist cart");
        }
    }

    private ShoppingCart deserialize(String json) {
        try {
            return objectMapper.readValue(json, ShoppingCart.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read cart");
        }
    }

    private String userKey(String userId)   { return USER_KEY_FORMAT.formatted(userId); }
    private String guestKey(String guestId) { return GUEST_KEY_FORMAT.formatted(guestId); }
}
