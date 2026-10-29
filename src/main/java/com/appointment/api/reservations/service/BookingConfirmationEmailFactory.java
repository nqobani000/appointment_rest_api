package com.appointment.api.reservations.service;

import com.appointment.api.addresses.dao.Location;
import com.appointment.api.appointments.dao.Appointment;
import com.appointment.api.consultants.dao.Consultant;
import com.appointment.api.notifications.EmailMessage;
import com.appointment.api.notifications.template.EmailTemplate;
import com.appointment.api.reservations.config.ReservationProperties;
import com.appointment.api.users.dao.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingConfirmationEmailFactory {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    private final ReservationProperties properties;

    public Optional<EmailMessage> build(Appointment appointment) {
        Customer customer = appointment.getCustomer();
        if (customer == null || !StringUtils.hasText(customer.getEmail())) {
            log.warn("Appointment {} has no customer email; skipping the confirmation email.",
                    appointment.getReferenceNo());
            return Optional.empty();
        }

        var start = appointment.getStartTime().atZoneSameInstant(properties.zone());
        var end = appointment.getEndTime().atZoneSameInstant(properties.zone());

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("customerName", customer.getName());
        variables.put("reference", appointment.getReferenceNo());
        variables.put("service", appointment.getService().getName());
        variables.put("date", DATE.format(start));
        variables.put("startTime", TIME.format(start));
        variables.put("endTime", TIME.format(end));
        variables.put("branchName", appointment.getBranch().getName());
        variables.put("branchAddress", address(appointment.getBranch().getLocation()));
        variables.put("consultant", join(" ", appointment.getConsultant().getName(),
                appointment.getConsultant().getSurname()));

        return Optional.of(new EmailMessage(
                "appointment-confirmation:" + appointment.getReferenceNo(),
                customer.getEmail().trim(),
                EmailTemplate.BOOKING_CONFIRMATION,
                variables));
    }

    private static String address(Location location) {
        if (location == null) {
            return "";
        }
        String street = join(" ", location.getStreetNo(), location.getStreetName());
        String province = location.getProvince() == null ? null : location.getProvince().getName();
        return join(", ", street, location.getSuburb(), location.getCity(), province, location.getPostalCode());
    }

    private static String join(String separator, String... parts) {
        return Stream.of(parts)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining(separator));
    }
}
