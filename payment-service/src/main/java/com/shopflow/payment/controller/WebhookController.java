package com.shopflow.payment.controller;

import com.shopflow.payment.service.WebhookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/payments/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(
            HttpServletRequest request,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) throws IOException {

        String payload = new String(request.getInputStream().readAllBytes());
        webhookService.processWebhookEvent(payload, sigHeader);
        return ResponseEntity.ok("Received");
    }
}
