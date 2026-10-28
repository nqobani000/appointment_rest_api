package com.appointment.api.users.dao;

import com.appointment.api.users.CustomerType;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "surname", length = 100, nullable = false)
    private String surname;

    @Column(name = "email", length = 320, nullable = false)
    private String email;

    @Column(name = "phone", length = 32, nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", length = 20, nullable = false)
    private CustomerType customerType;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;
}