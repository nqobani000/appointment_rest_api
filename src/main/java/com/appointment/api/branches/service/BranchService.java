package com.appointment.api.branches.service;

import com.appointment.api.branches.dto.BranchResponsePage;
import com.appointment.api.branches.dto.BranchServiceCriteria;
import org.springframework.data.domain.Pageable;

public interface BranchService {
    /**
     * Retrieves a paginated list of branches based on the provided criteria.
     *
     * @param criteria the criteria to filter branches
     * @param pageable the pagination information
     * @return a paginated response of branches
     */
    BranchResponsePage getAllBranches(BranchServiceCriteria criteria, Pageable pageable);
}