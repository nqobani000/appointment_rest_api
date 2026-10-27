package com.appointment.api.customers.repository;

import com.appointment.api.customers.dao.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
}
