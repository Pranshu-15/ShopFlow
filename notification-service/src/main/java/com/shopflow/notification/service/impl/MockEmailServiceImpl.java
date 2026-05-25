package com.shopflow.notification.service.impl;

import com.shopflow.notification.dto.EmailRequest;
import com.shopflow.notification.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "notification.email.mock", havingValue = "true")
@Slf4j
public class MockEmailServiceImpl implements EmailService {

    @Override
    public void send(EmailRequest request) {
        log.info("[MOCK EMAIL] To: {} <{}> | Subject: {} | Template: {} | Variables: {}",
                request.getToName(),
                request.getToEmail(),
                request.getSubject(),
                request.getTemplateName(),
                request.getVariables());
    }
}
