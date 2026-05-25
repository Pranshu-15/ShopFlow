package com.shopflow.notification.service.impl;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.shopflow.notification.dto.EmailRequest;
import com.shopflow.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@ConditionalOnProperty(name = "notification.email.mock", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class SendGridEmailServiceImpl implements EmailService {

    private final TemplateEngine templateEngine;

    @Value("${sendgrid.api-key}")
    private String apiKey;

    @Value("${sendgrid.from-email}")
    private String fromEmail;

    @Value("${sendgrid.from-name}")
    private String fromName;

    @Override
    public void send(EmailRequest request) {
        try {
            Context ctx = new Context();
            if (request.getVariables() != null) {
                request.getVariables().forEach(ctx::setVariable);
            }
            String htmlBody = templateEngine.process(request.getTemplateName(), ctx);

            Mail mail = new Mail(
                    new Email(fromEmail, fromName),
                    request.getSubject(),
                    new Email(request.getToEmail(), request.getToName()),
                    new Content("text/html", htmlBody)
            );

            SendGrid sg = new SendGrid(apiKey);
            Request sgRequest = new Request();
            sgRequest.setMethod(Method.POST);
            sgRequest.setEndpoint("mail/send");
            sgRequest.setBody(mail.build());

            var response = sg.api(sgRequest);
            log.info("Email sent to {} | Status: {}", request.getToEmail(), response.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", request.getToEmail(), e.getMessage(), e);
            throw new RuntimeException("Email delivery failed", e);
        }
    }
}
