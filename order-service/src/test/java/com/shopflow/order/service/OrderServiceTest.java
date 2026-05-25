package com.shopflow.order.service;

import com.shopflow.order.dto.OrderItemRequest;
import com.shopflow.order.dto.OrderRequest;
import com.shopflow.order.dto.OrderResponse;
import com.shopflow.order.entity.Order;
import com.shopflow.order.entity.OrderItem;
import com.shopflow.order.enums.OrderStatus;
import com.shopflow.order.repository.OrderRepository;
import com.shopflow.order.saga.SagaOrchestrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SagaOrchestrator sagaOrchestrator;

    @InjectMocks
    private OrderService orderService;

    private static final String USER_ID = "test-user-123";

    private OrderRequest buildRequest() {
        OrderItemRequest item = OrderItemRequest.builder()
                .productId("prod-001")
                .productName("Blue Widget")
                .unitPrice(new BigDecimal("29.99"))
                .quantity(2)
                .build();
        return OrderRequest.builder()
                .currency("USD")
                .items(List.of(item))
                .build();
    }

    private Order buildSavedOrder() {
        OrderItem item = OrderItem.builder()
                .id(1L)
                .productId("prod-001")
                .productName("Blue Widget")
                .unitPrice(new BigDecimal("29.99"))
                .quantity(2)
                .build();
        Order order = Order.builder()
                .id(1L)
                .orderId("order-uuid-001")
                .userId(USER_ID)
                .sagaId("saga-uuid-001")
                .totalAmount(new BigDecimal("59.98"))
                .currency("USD")
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>(List.of(item)))
                .build();
        order.getItems().forEach(i -> item.setOrder(order));
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        return order;
    }

    @Test
    void createOrder_calculatesCorrectTotalAndStartsSaga() {
        Order saved = buildSavedOrder();
        when(orderRepository.save(any(Order.class))).thenReturn(saved);
        doNothing().when(sagaOrchestrator).startSaga(any(Order.class));

        OrderResponse response = orderService.createOrder(USER_ID, buildRequest());

        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("59.98");
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(sagaOrchestrator).startSaga(any(Order.class));
    }

    @Test
    void createOrder_setsItemsOnOrder() {
        Order saved = buildSavedOrder();
        when(orderRepository.save(any(Order.class))).thenReturn(saved);

        OrderResponse response = orderService.createOrder(USER_ID, buildRequest());

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductId()).isEqualTo("prod-001");
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void getOrdersByUser_returnsFilteredOrders() {
        when(orderRepository.findByUserId(USER_ID)).thenReturn(List.of(buildSavedOrder()));

        List<OrderResponse> orders = orderService.getOrdersByUser(USER_ID);

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void getOrderById_whenOwner_returnsResponse() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(buildSavedOrder()));

        OrderResponse response = orderService.getOrderById(1L, USER_ID);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void getOrderById_whenNotFound_throwsNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L, USER_ID))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getOrderById_whenDifferentUser_throwsForbidden() {
        Order order = buildSavedOrder();
        order.setUserId("other-user-456");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderById(1L, USER_ID))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void updateStatus_validTransition_updatesOrder() {
        Order order = buildSavedOrder();
        when(orderRepository.findByOrderId("order-uuid-001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponse response = orderService.updateStatus("order-uuid-001", OrderStatus.CONFIRMED);

        assertThat(response).isNotNull();
        verify(orderRepository).save(order);
    }

    @Test
    void updateStatus_invalidTransition_throwsConflict() {
        Order order = buildSavedOrder();
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findByOrderId("order-uuid-001")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus("order-uuid-001", OrderStatus.PENDING))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }
}
