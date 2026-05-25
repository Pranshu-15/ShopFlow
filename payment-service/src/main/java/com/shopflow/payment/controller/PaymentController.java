package com.shopflow.payment.controller;

import com.shopflow.payment.dto.PaymentRequest;
import com.shopflow.payment.dto.PaymentResponse;
import com.shopflow.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> initiatePayment(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.processPayment(jwt.getSubject(), request));
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getMyPayments(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(paymentService.getPaymentsByUser(jwt.getSubject()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id, jwt.getSubject()));
    }
}
