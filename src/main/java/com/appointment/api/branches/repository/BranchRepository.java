
package com.appointment.api.branches.repository;

import com.appointment.api.branches.dao.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BranchRepository extends JpaRepository<Branch, UUID> {

    @Query(
            value = """
                    SELECT b.*
                    FROM branches b
                    JOIN locations l ON l.id = b.location_id
                    WHERE (
                        CAST(:search AS text) IS NULL
                        OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                        OR LOWER(COALESCE(l.suburb, ''))
                            LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                        OR LOWER(COALESCE(l.city, ''))
                            LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                        OR LOWER(COALESCE(l.postal_code, ''))
                            LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                    )
                    ORDER BY
                        CASE
                            WHEN CAST(:latitude AS double precision) IS NOT NULL
                             AND CAST(:longitude AS double precision) IS NOT NULL
                             AND l.coordinates IS NOT NULL
                            THEN ST_Distance(
                                l.coordinates,
                                CAST(
                                    ST_SetSRID(
                                        ST_MakePoint(:longitude, :latitude),
                                        4326
                                    ) AS geography
                                )
                            )
                            ELSE NULL
                        END ASC NULLS LAST,
                        b.name ASC
                    """,
            countQuery = """
            SELECT COUNT(*)
            FROM branches b
            JOIN locations l ON l.id = b.location_id
            WHERE (
                CAST(:search AS text) IS NULL
                OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                OR LOWER(COALESCE(l.suburb, ''))
                    LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                OR LOWER(COALESCE(l.city, ''))
                    LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                OR LOWER(COALESCE(l.postal_code, ''))
                    LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
            )
            """,
            nativeQuery = true
    )
    Page<Branch> findAllBranches(
            @Param("search") String search,
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            Pageable pageable
    );
}
