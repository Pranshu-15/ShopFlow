package com.shopflow.cart.controller;

import com.shopflow.cart.dto.AddItemRequest;
import com.shopflow.cart.dto.UpdateQuantityRequest;
import com.shopflow.cart.model.ShoppingCart;
import com.shopflow.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // ── Authenticated user cart ─────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ShoppingCart> getUserCart(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(cartService.getUserCart(jwt.getSubject()));
    }

    @PostMapping("/items")
    public ResponseEntity<ShoppingCart> addItemToUserCart(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddItemRequest request) {
        return ResponseEntity.ok(cartService.addItemToUserCart(jwt.getSubject(), request));
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<ShoppingCart> updateUserCartItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateQuantityRequest request) {
        return ResponseEntity.ok(cartService.updateUserCartItemQuantity(jwt.getSubject(), productId, request));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<ShoppingCart> removeItemFromUserCart(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long productId) {
        return ResponseEntity.ok(cartService.removeItemFromUserCart(jwt.getSubject(), productId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearUserCart(@AuthenticationPrincipal Jwt jwt) {
        cartService.clearUserCart(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/merge")
    public ResponseEntity<ShoppingCart> mergeGuestCart(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String guestId) {
        return ResponseEntity.ok(cartService.mergeGuestCartIntoUserCart(jwt.getSubject(), guestId));
    }

    // ── Guest cart ──────────────────────────────────────────────────────────

    @GetMapping("/guest/{guestId}")
    public ResponseEntity<ShoppingCart> getGuestCart(@PathVariable String guestId) {
        return ResponseEntity.ok(cartService.getGuestCart(guestId));
    }

    @PostMapping("/guest/{guestId}/items")
    public ResponseEntity<ShoppingCart> addItemToGuestCart(
            @PathVariable String guestId,
            @Valid @RequestBody AddItemRequest request) {
        return ResponseEntity.ok(cartService.addItemToGuestCart(guestId, request));
    }

    @PutMapping("/guest/{guestId}/items/{productId}")
    public ResponseEntity<ShoppingCart> updateGuestCartItem(
            @PathVariable String guestId,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateQuantityRequest request) {
        return ResponseEntity.ok(cartService.updateGuestCartItemQuantity(guestId, productId, request));
    }

    @DeleteMapping("/guest/{guestId}/items/{productId}")
    public ResponseEntity<ShoppingCart> removeItemFromGuestCart(
            @PathVariable String guestId,
            @PathVariable Long productId) {
        return ResponseEntity.ok(cartService.removeItemFromGuestCart(guestId, productId));
    }

    @DeleteMapping("/guest/{guestId}")
    public ResponseEntity<Void> clearGuestCart(@PathVariable String guestId) {
        cartService.clearGuestCart(guestId);
        return ResponseEntity.noContent().build();
    }
}
