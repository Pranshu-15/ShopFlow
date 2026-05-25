package com.shopflow.order.service;

import com.shopflow.order.dto.OrderItemResponse;
import com.shopflow.order.dto.OrderRequest;
import com.shopflow.order.dto.OrderResponse;
import com.shopflow.order.entity.Order;
import com.shopflow.order.entity.OrderItem;
import com.shopflow.order.enums.OrderStatus;
import com.shopflow.order.repository.OrderRepository;
import com.shopflow.order.saga.SagaOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final SagaOrchestrator sagaOrchestrator;

    @Transactional
    public OrderResponse createOrder(String userId, OrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        String sagaId  = UUID.randomUUID().toString();

        BigDecimal totalAmount = request.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .orderId(orderId)
                .userId(userId)
                .sagaId(sagaId)
                .totalAmount(totalAmount)
                .currency(request.getCurrency())
                .status(OrderStatus.PENDING)
                .build();

        List<OrderItem> items = request.getItems().stream()
                .map(i -> OrderItem.builder()
                        .order(order)
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .unitPrice(i.getUnitPrice())
                        .quantity(i.getQuantity())
                        .build())
                .toList();

        order.getItems().addAll(items);
        Order saved = orderRepository.save(order);

        sagaOrchestrator.startSaga(saved);

        return toResponse(saved);
    }

    public List<OrderResponse> findAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<OrderResponse> getOrdersByUser(String userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public OrderResponse getOrderById(Long id, String userId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return toResponse(order);
    }

    // State transitions — callable by other services or admin endpoints in future phases
    @Transactional
    public OrderResponse updateStatus(String orderId, OrderStatus newStatus) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        validateTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);
        return toResponse(orderRepository.save(order));
    }

    private void validateTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING    -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED || next == OrderStatus.FAILED;
            case CONFIRMED  -> next == OrderStatus.SHIPPED   || next == OrderStatus.CANCELLED || next == OrderStatus.FAILED;
            case SHIPPED    -> false;
            case CANCELLED  -> false;
            case FAILED     -> false;
        };
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Invalid transition: " + current + " → " + next);
        }
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(i -> OrderItemResponse.builder()
                        .id(i.getId())
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .unitPrice(i.getUnitPrice())
                        .quantity(i.getQuantity())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .status(order.getStatus())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
