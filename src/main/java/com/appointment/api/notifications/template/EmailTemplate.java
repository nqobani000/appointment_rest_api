package com.appointment.api.notifications.template;

import java.util.Map;

public enum EmailTemplate {
    VERIFICATION_CODE("Your verification code",
            "Your verification code is {code}. It expires in {minutes} minutes. "
                    + "If you did not request it, you can ignore this email.");

    private final String subject;
    private final String body;

    EmailTemplate(String subject, String body) {
        this.subject = subject;
        this.body = body;
    }

    public String subject() {
        return subject;
    }

    public String renderBody(Map<String, String> variables) {
        String result = body;
        for (Map.Entry<String, String> e : variables.entrySet()) {
            result = result.replace("{" + e.getKey() + "}", e.getValue());
        }
        return result;
    }
}
