package com.appointment.api.addresses.dao;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;
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

    @Column(name = "street_no", length = 20)
    private String streetNo;

    @Column(name = "street_name", length = 150)
    private String streetName;

    @Column(name = "suburb", length = 100)
    private String suburb;

    @Column(name = "city", length = 100, nullable = false)
    private String city;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "province", referencedColumnName = "code", nullable = false)
    private Province province;

    @Column(name = "postal_code", length = 16)
    private String postalCode;

    @Column(name = "country", length = 100, nullable = false)
    private String country = "South Africa";

    @JdbcTypeCode(SqlTypes.GEOGRAPHY)
    @Column(name = "coordinates", columnDefinition = "geography(Point,4326)")
    private Point coordinates;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}