package com.shopflow.notification.service;

import com.shopflow.notification.dto.EmailRequest;

public interface EmailService {

    void send(EmailRequest request);
}
