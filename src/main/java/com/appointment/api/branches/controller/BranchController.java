package com.appointment.api.branches.controller;

import com.appointment.api.branches.dto.BranchResponsePage;
import com.appointment.api.branches.dto.BranchServiceCriteria;
import com.appointment.api.branches.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    public ResponseEntity<BranchResponsePage> getAllBranches(
            @ModelAttribute BranchServiceCriteria criteria, Pageable pageable) {
        return ResponseEntity.ok(branchService.getAllBranches(criteria, pageable));
    }
}
