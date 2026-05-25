package com.shopflow.user.controller;

import com.shopflow.user.dto.UserAddressRequest;
import com.shopflow.user.dto.UserAddressResponse;
import com.shopflow.user.service.UserAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService addressService;

    @GetMapping
    public ResponseEntity<List<UserAddressResponse>> getAllAddresses(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(addressService.getUserAddresses(userId));
    }

    @GetMapping("/default")
    public ResponseEntity<UserAddressResponse> getDefaultAddress(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(addressService.getDefaultAddress(userId));
    }

    @PostMapping
    public ResponseEntity<UserAddressResponse> createAddress(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserAddressRequest request) {
        String userId = jwt.getSubject();
        UserAddressResponse created = addressService.createAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<UserAddressResponse> updateAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long addressId,
            @Valid @RequestBody UserAddressRequest request) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(addressService.updateAddress(userId, addressId, request));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long addressId) {
        String userId = jwt.getSubject();
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.noContent().build();
    }
}
