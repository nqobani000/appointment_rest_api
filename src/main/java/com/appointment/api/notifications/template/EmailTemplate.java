package com.appointment.api.notifications.template;

import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Placeholders use the {@code {name}} form. HTML rendering escapes every substituted value, so user-supplied text
 * (names, addresses) can never inject markup. Every placeholder must have a value; rendering fails otherwise.
 */
public enum EmailTemplate {
    VERIFICATION_CODE(
            "Your access code",
            "Your access code is <b>{code}</b> and it expires in <b>{expiresIn}</b>",
            "Your access code is {code} and it expires in {expiresIn}"),

    BOOKING_CONFIRMATION(
            "Appointment confirmed - {reference}",
            """
                    <p>Hi {customerName},</p>
                    <p>Your appointment is confirmed.</p>
                    <table cellpadding="4">
                      <tr><td><b>Reference</b></td><td>{reference}</td></tr>
                      <tr><td><b>Service</b></td><td>{service}</td></tr>
                      <tr><td><b>Date</b></td><td>{date}</td></tr>
                      <tr><td><b>Time</b></td><td>{startTime} - {endTime}</td></tr>
                      <tr><td><b>Branch</b></td><td>{branchName}</td></tr>
                      <tr><td><b>Address</b></td><td>{branchAddress}</td></tr>
                      <tr><td><b>Consultant</b></td><td>{consultant}</td></tr>
                    </table>
                    <p>Please quote your reference number when you arrive at the branch.</p>""",
            """
                    Hi {customerName},

                    Your appointment is confirmed.

                    Reference: {reference}
                    Service: {service}
                    Date: {date}
                    Time: {startTime} - {endTime}
                    Branch: {branchName}
                    Address: {branchAddress}
                    Consultant: {consultant}

                    Please quote your reference number when you arrive at the branch.""");

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\w+)}");

    private final String subject;
    private final String htmlBody;
    private final String textBody;

    EmailTemplate(String subject, String htmlBody, String textBody) {
        this.subject = subject;
        this.htmlBody = htmlBody;
        this.textBody = textBody;
    }

    /** Fails if any placeholder has no value, so a message with raw "{code}" text can never be queued or sent. */
    public void validate(Map<String, String> variables) {
        Set<String> missing = new TreeSet<>();
        for (String template : List.of(subject, htmlBody, textBody)) {
            Matcher matcher = PLACEHOLDER.matcher(template);
            while (matcher.find()) {
                if (variables == null || variables.get(matcher.group(1)) == null) {
                    missing.add(matcher.group(1));
                }
            }
        }
        if (!missing.isEmpty()) {
            // Names only: values can be secrets such as verification codes.
            throw new IllegalArgumentException("Email template " + name() + " is missing variables " + missing);
        }
    }

    public String renderSubject(Map<String, String> variables) {
        return render(subject, variables, UnaryOperator.identity());
    }

    public String renderHtml(Map<String, String> variables) {
        return render(htmlBody, variables, HtmlUtils::htmlEscape);
    }

    public String renderText(Map<String, String> variables) {
        return render(textBody, variables, UnaryOperator.identity());
    }

    // Single pass, so a substituted value that itself contains "{...}" is never re-expanded.
    private String render(String template, Map<String, String> variables, UnaryOperator<String> escaper) {
        validate(variables);
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String replacement = escaper.apply(variables.get(matcher.group(1)));
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
