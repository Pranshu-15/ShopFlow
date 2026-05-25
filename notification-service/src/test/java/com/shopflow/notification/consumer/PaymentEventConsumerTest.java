package com.shopflow.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.notification.dto.EmailRequest;
import com.shopflow.notification.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class PaymentEventConsumerTest {

    @Mock
    private EmailService emailService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentEventConsumer consumer;

    private final ObjectMapper realMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void consume_paymentSucceededEvent_sendsReceiptEmail() throws Exception {
        String payload = """
                {"type":"PAYMENT_RESULT","sagaId":"s-1","orderId":"o-1","userId":"user-1",
                 "status":"SUCCEEDED","amount":"49.99","currency":"USD"}
                """;

        var field = PaymentEventConsumer.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(consumer, realMapper);

        consumer.consume(payload);

        ArgumentCaptor<EmailRequest> captor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailService).send(captor.capture());

        EmailRequest sent = captor.getValue();
        assertThat(sent.getSubject()).contains("Receipt");
        assertThat(sent.getTemplateName()).isEqualTo("paymentReceipt");
        assertThat(sent.getVariables()).containsEntry("status", "SUCCEEDED");
    }

    @Test
    void consume_paymentFailedEvent_sendsFailureEmail() throws Exception {
        String payload = """
                {"type":"PAYMENT_RESULT","sagaId":"s-1","orderId":"o-2","userId":"user-1",
                 "status":"FAILED","reason":"Card declined"}
                """;

        var field = PaymentEventConsumer.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(consumer, realMapper);

        consumer.consume(payload);

        ArgumentCaptor<EmailRequest> captor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailService).send(captor.capture());

        EmailRequest sent = captor.getValue();
        assertThat(sent.getSubject()).contains("Failed");
        assertThat(sent.getVariables()).containsEntry("status", "FAILED");
        assertThat(sent.getVariables()).containsEntry("reason", "Card declined");
    }

    @Test
    void consume_nonPaymentResultEvent_doesNotSendEmail() throws Exception {
        // PAYMENT_REQUEST events published by order service — should be ignored
        String payload = """
                {"type":"PAYMENT_REQUEST","sagaId":"s-1","orderId":"o-3","userId":"user-1",
                 "amount":"29.99","currency":"USD"}
                """;

        var field = PaymentEventConsumer.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(consumer, realMapper);

        consumer.consume(payload);

        verify(emailService, never()).send(any());
    }

    @Test
    void consume_dlqScenario_throwsOnProcessingError() throws Exception {
        // Malformed payload triggers exception, which the KafkaErrorHandler routes to DLT
        var field = PaymentEventConsumer.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(consumer, realMapper);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> consumer.consume("{ invalid json !!"));
    }
}
