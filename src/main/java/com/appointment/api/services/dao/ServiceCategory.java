package com.appointment.api.services.dao;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service_categories")
@Getter
@Setter
@NoArgsConstructor
public class ServiceCategory {
    @Id
    @Column(name = "code", length = 40, nullable = false, updatable = false)
    private String code;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @Column(name = "sort_order", nullable = false, unique = true)
    private Short sortOrder;
}