package com.shopflow.order.repository;

import com.shopflow.order.entity.SagaLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SagaLogRepository extends JpaRepository<SagaLog, Long> {

    List<SagaLog> findBySagaId(String sagaId);

    List<SagaLog> findByOrderId(String orderId);
}
