package com.shopflow.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {

    private String toEmail;
    private String toName;
    private String subject;
    private String templateName;
    private Map<String, Object> variables;
}
