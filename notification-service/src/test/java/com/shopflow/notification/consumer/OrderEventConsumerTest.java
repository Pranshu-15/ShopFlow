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
class OrderEventConsumerTest {

    @Mock
    private EmailService emailService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderEventConsumer consumer;

    private final ObjectMapper realMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void consume_orderCreatedEvent_sendsOrderReceivedEmail() throws Exception {
        String payload = """
                {"type":"ORDER_CREATED","sagaId":"s-1","orderId":"o-1","userId":"user-1",
                 "amount":"99.99","currency":"USD"}
                """;

        // Use real mapper to parse, bypass mock
        var field = OrderEventConsumer.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(consumer, realMapper);

        consumer.consume(payload);

        ArgumentCaptor<EmailRequest> captor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailService).send(captor.capture());

        EmailRequest sent = captor.getValue();
        assertThat(sent.getSubject()).contains("o-1");
        assertThat(sent.getTemplateName()).isEqualTo("orderConfirmation");
        assertThat(sent.getVariables()).containsEntry("orderId", "o-1");
        assertThat(sent.getVariables()).containsEntry("eventType", "ORDER_RECEIVED");
    }

    @Test
    void consume_orderConfirmedEvent_sendsConfirmedEmail() throws Exception {
        String payload = """
                {"type":"ORDER_CONFIRMED","sagaId":"s-1","orderId":"o-2","userId":"user-1",
                 "amount":"49.99","currency":"USD"}
                """;

        var field = OrderEventConsumer.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(consumer, realMapper);

        consumer.consume(payload);

        ArgumentCaptor<EmailRequest> captor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailService).send(captor.capture());

        EmailRequest sent = captor.getValue();
        assertThat(sent.getSubject()).contains("Confirmed");
        assertThat(sent.getVariables()).containsEntry("eventType", "ORDER_CONFIRMED");
    }

    @Test
    void consume_unknownEventType_doesNotSendEmail() throws Exception {
        String payload = """
                {"type":"ORDER_SHIPPED","orderId":"o-3","userId":"user-1"}
                """;

        var field = OrderEventConsumer.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(consumer, realMapper);

        consumer.consume(payload);

        verify(emailService, never()).send(any());
    }
}
