
package com.appointment.api.services.repository;

import com.appointment.api.services.dao.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServicesRepository extends JpaRepository<Service, UUID> {

    @Query(
            value = """
                    SELECT s
                    FROM Service s
                    JOIN FETCH s.category c
                    WHERE s.active = true
                      AND (
                          :filter = ''
                          OR LOWER(s.name) LIKE LOWER(CONCAT('%', :filter, '%'))
                          OR LOWER(COALESCE(s.description, '')) LIKE LOWER(CONCAT('%', :filter, '%'))
                          OR LOWER(c.name) LIKE LOWER(CONCAT('%', :filter, '%'))
                      )
                    ORDER BY c.sortOrder ASC, s.name ASC, s.id ASC
                    """,
            countQuery = """
                    SELECT COUNT(s)
                    FROM Service s
                    JOIN s.category c
                    WHERE s.active = true
                      AND (
                          :filter = ''
                          OR LOWER(s.name) LIKE LOWER(CONCAT('%', :filter, '%'))
                          OR LOWER(COALESCE(s.description, '')) LIKE LOWER(CONCAT('%', :filter, '%'))
                          OR LOWER(c.name) LIKE LOWER(CONCAT('%', :filter, '%'))
                      )
                    """
    )
    Page<Service> findActiveServices(@Param("filter") String filter, Pageable pageable);

    @Query(
            value = """
                    SELECT s
                    FROM BranchService bs
                    JOIN bs.service s
                    JOIN FETCH s.category c
                    WHERE bs.branch.id = :branchId
                      AND s.active = true
                      AND (
                          :filter = ''
                          OR LOWER(s.name) LIKE LOWER(CONCAT('%', :filter, '%'))
                          OR LOWER(COALESCE(s.description, '')) LIKE LOWER(CONCAT('%', :filter, '%'))
                          OR LOWER(c.name) LIKE LOWER(CONCAT('%', :filter, '%'))
                      )
                    ORDER BY c.sortOrder ASC, s.name ASC, s.id ASC
                    """,
            countQuery = """
                    SELECT COUNT(s)
                    FROM BranchService bs
                    JOIN bs.service s
                    JOIN s.category c
                    WHERE bs.branch.id = :branchId
                      AND s.active = true
                      AND (
                          :filter = ''
                          OR LOWER(s.name) LIKE LOWER(CONCAT('%', :filter, '%'))
                          OR LOWER(COALESCE(s.description, '')) LIKE LOWER(CONCAT('%', :filter, '%'))
                          OR LOWER(c.name) LIKE LOWER(CONCAT('%', :filter, '%'))
                      )
                    """
    )
    Page<Service> findActiveServicesByBranchId(@Param("branchId") UUID branchId,
                                               @Param("filter") String filter,
                                               Pageable pageable);

    @Query("""
            SELECT s
            FROM Service s
            JOIN FETCH s.category
            WHERE s.id = :serviceId
              AND s.active = true
            """)
    Optional<Service> findActiveServiceById(@Param("serviceId") UUID serviceId);
}
