package com.appointment.api.branches.repository;

import com.appointment.api.branches.dao.BranchService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BranchServiceLinkRepository extends JpaRepository<BranchService, UUID> {
    boolean existsByBranchIdAndServiceId(UUID branchId, UUID serviceId);
}
