package com.appointment.api.branches.service;

import com.appointment.api.addresses.dao.Location;
import com.appointment.api.branches.dao.Branch;
import com.appointment.api.branches.dto.BranchResponse;
import com.appointment.api.branches.dto.BranchServiceCriteria;
import com.appointment.api.branches.mapper.BranchMapper;
import com.appointment.api.branches.repository.BranchRepository;
import com.appointment.api.exceptions.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchServiceImplTest {

    @Mock
    BranchRepository repository;
    @Mock
    BranchMapper mapper;
    @InjectMocks
    BranchServiceImpl service;

    @Test
    void escapesLikeWildcardsAndDropsClientSort() {
        when(repository.findAllBranches(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        service.getAllBranches(new BranchServiceCriteria(" 50%_off ", null, null),
                PageRequest.of(1, 5, Sort.by("name")));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAllBranches(eq("50\\%\\_off"), eq(null), eq(null), pageable.capture());
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.unsorted());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
    }

    @Test
    void blankSearchBecomesNull() {
        when(repository.findAllBranches(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        service.getAllBranches(new BranchServiceCriteria("   ", null, null), PageRequest.of(0, 5));
        verify(repository).findAllBranches(eq(null), eq(null), eq(null), any());
    }

    @Test
    void distanceIsNullWithoutCoordinates() {
        Branch branch = new Branch();
        when(repository.findAllBranches(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(branch)));
        when(mapper.toResponse(branch, null)).thenReturn(new BranchResponse());

        assertThat(service.getAllBranches(new BranchServiceCriteria(null, -26.2, 28.0), PageRequest.of(0, 5))
                .getContent()).hasSize(1);
    }

    @Test
    void distanceIsRoundedToTwoDecimals() {
        Location location = new Location();
        location.setCoordinates(new GeometryFactory().createPoint(new Coordinate(28.0458588, -26.2031266)));
        Branch branch = new Branch();
        branch.setLocation(location);
        when(repository.findAllBranches(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(branch)));
        when(mapper.toResponse(eq(branch), any())).thenReturn(new BranchResponse());

        service.getAllBranches(new BranchServiceCriteria(null, -26.2, 28.0), PageRequest.of(0, 5));

        ArgumentCaptor<Double> distance = ArgumentCaptor.forClass(Double.class);
        verify(mapper).toResponse(eq(branch), distance.capture());
        assertThat(distance.getValue()).isEqualTo(4.59);
    }

    @Test
    void rejectsInvalidCriteria() {
        Pageable page = PageRequest.of(0, 5);
        assertThatThrownBy(() -> service.getAllBranches(new BranchServiceCriteria(null, 1.0, null), page))
                .isInstanceOf(InvalidArgumentException.class);
        assertThatThrownBy(() -> service.getAllBranches(new BranchServiceCriteria(null, 91.0, 0.0), page))
                .isInstanceOf(InvalidArgumentException.class);
        assertThatThrownBy(() -> service.getAllBranches(new BranchServiceCriteria("x".repeat(101), null, null), page))
                .isInstanceOf(InvalidArgumentException.class);
        verifyNoInteractions(repository);
    }
}
