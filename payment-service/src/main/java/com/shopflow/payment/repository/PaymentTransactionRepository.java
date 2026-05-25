package com.shopflow.payment.repository;

import com.shopflow.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByUserId(String userId);

    List<PaymentTransaction> findByOrderId(String orderId);

    Optional<PaymentTransaction> findByStripePaymentIntentId(String stripePaymentIntentId);
}
