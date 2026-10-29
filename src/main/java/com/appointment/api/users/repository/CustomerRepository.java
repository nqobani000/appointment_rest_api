package com.appointment.api.users.repository;

import com.appointment.api.users.dao.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByUserId(UUID userId);

    Optional<Customer> findByEmailIgnoreCaseAndCustomerType(String email, com.appointment.api.users.CustomerType customerType);
}
