package com.appointment.api.addresses.dao;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "street_no", length = 20, nullable = false)
    private String streetNo;

    @Column(name = "street_name", length = 150, nullable = false)
    private String streetName;

    @Column(name = "suburb", length = 100, nullable = false)
    private String suburb;

    @Column(name = "city", length = 100, nullable = false)
    private String city;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "province", referencedColumnName = "code", nullable = false)
    private Province province;

    @Column(name = "postal_code", length = 16, nullable = false)
    private String postalCode;

    @Column(name = "country", length = 100, nullable = false)
    private String country = "South Africa";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}