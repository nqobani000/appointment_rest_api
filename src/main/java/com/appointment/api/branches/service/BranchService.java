package com.appointment.api.branches.service;

import com.appointment.api.branches.dto.BranchResponsePage;
import com.appointment.api.branches.dto.BranchServiceCriteria;
import org.springframework.data.domain.Pageable;

public interface BranchService {

    BranchResponsePage getAllBranches(BranchServiceCriteria criteria, Pageable pageable);

}
