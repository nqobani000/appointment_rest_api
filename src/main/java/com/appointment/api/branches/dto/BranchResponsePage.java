package com.appointment.api.branches.dto;

import com.appointment.api.commons.dto.PageResponse;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BranchResponsePage extends PageResponse {
    private List<BranchResponse> content;

    public BranchResponsePage(List<BranchResponse> content,
                              int page, int size, long totalElements, int totalPages,
                              boolean first, boolean last) {
        super(page, size, totalElements, totalPages, first, last);
        this.content = content;
    }
}
