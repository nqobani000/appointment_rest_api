package com.appointment.api.notifications;

import com.appointment.api.notifications.template.EmailTemplate;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTemplateTest {

    @Test
    void verificationCodeHtmlMatchesTheRequiredFormat() {
        String html = EmailTemplate.VERIFICATION_CODE.renderHtml(Map.of("code", "123456", "expiresIn", "10 minutes"));
        assertThat(html).isEqualTo("Your access code is <b>123456</b> and it expires in <b>10 minutes</b>");
    }

    @Test
    void verificationCodeTextHasNoMarkup() {
        String text = EmailTemplate.VERIFICATION_CODE.renderText(Map.of("code", "123456", "expiresIn", "10 minutes"));
        assertThat(text).isEqualTo("Your access code is 123456 and it expires in 10 minutes");
    }

    @Test
    void htmlRenderingEscapesVariablesButTextDoesNot() {
        Map<String, String> vars = Map.of("code", "<script>x</script>", "expiresIn", "10 minutes");
        assertThat(EmailTemplate.VERIFICATION_CODE.renderHtml(vars))
                .contains("&lt;script&gt;x&lt;/script&gt;")
                .doesNotContain("<script>");
        assertThat(EmailTemplate.VERIFICATION_CODE.renderText(vars)).contains("<script>x</script>");
    }

    @Test
    void substitutedValuesAreNotReExpanded() {
        Map<String, String> vars = Map.of("code", "{expiresIn}", "expiresIn", "10 minutes");
        assertThat(EmailTemplate.VERIFICATION_CODE.renderText(vars))
                .isEqualTo("Your access code is {expiresIn} and it expires in 10 minutes");
    }

    @Test
    void valuesContainingRegexReplacementCharactersAreKeptLiteral() {
        Map<String, String> vars = Map.of("code", "$1\\", "expiresIn", "10 minutes");
        assertThat(EmailTemplate.VERIFICATION_CODE.renderText(vars)).contains("$1\\");
    }

    @Test
    void missingVariablesFailInsteadOfLeavingRawPlaceholders() {
        assertThatThrownBy(() -> EmailTemplate.VERIFICATION_CODE.renderText(Map.of("code", "123456")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiresIn")
                .hasMessageNotContaining("123456");
        assertThatThrownBy(() -> EmailTemplate.VERIFICATION_CODE.renderHtml(Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll("code", "expiresIn");
        assertThatThrownBy(() -> EmailTemplate.VERIFICATION_CODE.renderSubject(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void oldStyleVariablesAreRejected() {
        assertThatThrownBy(() -> EmailTemplate.VERIFICATION_CODE.validate(Map.of("code", "123456", "minutes", "10")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiresIn");
    }

    @Test
    void bookingConfirmationRendersEveryFieldInSubjectHtmlAndText() {
        Map<String, String> vars = Map.of(
                "customerName", "Jane", "reference", "APT-20300101-ABC", "service", "Open an account",
                "date", "Tuesday, 1 January 2030", "startTime", "10:00", "endTime", "10:30",
                "branchName", "Sandton", "branchAddress", "1 Main Rd, Sandton", "consultant", "Thabo Mokoena");

        assertThat(EmailTemplate.BOOKING_CONFIRMATION.renderSubject(vars)).isEqualTo("Appointment confirmed - APT-20300101-ABC");
        for (String rendered : new String[]{
                EmailTemplate.BOOKING_CONFIRMATION.renderHtml(vars),
                EmailTemplate.BOOKING_CONFIRMATION.renderText(vars)}) {
            assertThat(rendered).contains("Jane", "APT-20300101-ABC", "Open an account", "Tuesday, 1 January 2030",
                    "10:00 - 10:30", "Sandton", "1 Main Rd, Sandton", "Thabo Mokoena")
                    .doesNotContain("{");
        }
        assertThat(EmailTemplate.BOOKING_CONFIRMATION.renderText(vars)).doesNotContain("<");
    }

    @Test
    void bookingConfirmationHtmlEscapesUserSuppliedText() {
        Map<String, String> vars = Map.of(
                "customerName", "<img src=x onerror=alert(1)>", "reference", "R", "service", "S",
                "date", "D", "startTime", "1", "endTime", "2", "branchName", "B", "branchAddress", "A",
                "consultant", "C");
        assertThat(EmailTemplate.BOOKING_CONFIRMATION.renderHtml(vars))
                .contains("&lt;img src=x onerror=alert(1)&gt;")
                .doesNotContain("<img");
    }
}
