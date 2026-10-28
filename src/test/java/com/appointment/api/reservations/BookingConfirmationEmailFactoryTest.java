package com.appointment.api.reservations;

import com.appointment.api.addresses.dao.Location;
import com.appointment.api.addresses.dao.Province;
import com.appointment.api.appointments.dao.Appointment;
import com.appointment.api.branches.dao.Branch;
import com.appointment.api.consultants.dao.Consultant;
import com.appointment.api.notifications.EmailMessage;
import com.appointment.api.notifications.template.EmailTemplate;
import com.appointment.api.reservations.config.ReservationProperties;
import com.appointment.api.reservations.service.BookingConfirmationEmailFactory;
import com.appointment.api.services.dao.Service;
import com.appointment.api.users.dao.Customer;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BookingConfirmationEmailFactoryTest {

    final BookingConfirmationEmailFactory sut = new BookingConfirmationEmailFactory(
            new ReservationProperties(Duration.ofMinutes(10), ZoneId.of("Africa/Johannesburg"), Duration.ofMinutes(1)));

    Appointment appointment(String email, Location location) {
        Customer customer = new Customer();
        customer.setName("Jane");
        customer.setEmail(email);
        Branch branch = new Branch();
        branch.setName("Sandton");
        branch.setLocation(location);
        Service service = new Service();
        service.setName("Open an account");
        Consultant consultant = new Consultant();
        consultant.setName("Thabo");
        consultant.setSurname("Mokoena");
        return Appointment.builder()
                .referenceNo("APT-20300101-ABC")
                .customer(customer).branch(branch).service(service).consultant(consultant)
                .startTime(OffsetDateTime.parse("2030-01-01T08:00:00Z"))
                .endTime(OffsetDateTime.parse("2030-01-01T08:30:00Z"))
                .build();
    }

    Location location(String streetNo, String suburb) {
        Province province = new Province();
        province.setName("Gauteng");
        Location location = new Location();
        location.setStreetNo(streetNo);
        location.setStreetName("Rivonia Road");
        location.setSuburb(suburb);
        location.setCity("Johannesburg");
        location.setProvince(province);
        location.setPostalCode("2196");
        return location;
    }

    @Test
    void buildsTheConfirmationInTheReservationZone() {
        EmailMessage message = sut.build(appointment(" jane@example.com ", location("12", "Sandton"))).orElseThrow();

        assertThat(message.recipient()).isEqualTo("jane@example.com");
        assertThat(message.template()).isEqualTo(EmailTemplate.BOOKING_CONFIRMATION);
        assertThat(message.dedupeKey()).isEqualTo("appointment-confirmation:APT-20300101-ABC");
        assertThat(message.variables())
                .containsEntry("customerName", "Jane")
                .containsEntry("reference", "APT-20300101-ABC")
                .containsEntry("service", "Open an account")
                .containsEntry("branchName", "Sandton")
                .containsEntry("consultant", "Thabo Mokoena")
                .containsEntry("date", "Tuesday, 1 January 2030")
                .containsEntry("startTime", "10:00")
                .containsEntry("endTime", "10:30")
                .containsEntry("branchAddress", "12 Rivonia Road, Sandton, Johannesburg, Gauteng, 2196");
    }

    @Test
    void skipsMissingAddressPartsWithoutStrayCommas() {
        EmailMessage message = sut.build(appointment("jane@example.com", location(null, null))).orElseThrow();
        assertThat(message.variables()).containsEntry("branchAddress", "Rivonia Road, Johannesburg, Gauteng, 2196");
    }

    @Test
    void toleratesABranchWithoutALocation() {
        EmailMessage message = sut.build(appointment("jane@example.com", null)).orElseThrow();
        assertThat(message.variables()).containsEntry("branchAddress", "");
    }

    @Test
    void returnsEmptyWhenTheCustomerHasNoEmail() {
        assertThat(sut.build(appointment("  ", null))).isEqualTo(Optional.empty());
        assertThat(sut.build(appointment(null, null))).isEqualTo(Optional.empty());
    }
}
