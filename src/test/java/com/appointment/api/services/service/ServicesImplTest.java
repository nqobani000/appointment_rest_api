package com.appointment.api.services.service;

import com.appointment.api.customers.CustomerType;
import com.appointment.api.exceptions.InvalidArgumentException;
import com.appointment.api.exceptions.ServiceNotFoundException;
import com.appointment.api.services.dao.ServiceCategory;
import com.appointment.api.services.dto.ServiceResponse;
import com.appointment.api.services.dto.ServiceResponsePage;
import com.appointment.api.services.dto.ServiceSearchCriteria;
import com.appointment.api.services.mapper.ServiceMapper;
import com.appointment.api.services.repository.ServicesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicesImplTest {

    @Mock
    private ServicesRepository servicesRepository;

    @Spy
    private ServiceMapper serviceMapper = new ServiceMapper();

    @InjectMocks
    private ServicesImpl servicesImpl;

    private static com.appointment.api.services.dao.Service service(String name, String categoryName,
                                                                    boolean registered, boolean guest) {
        ServiceCategory category = new ServiceCategory();
        category.setCode(categoryName.toUpperCase());
        category.setName(categoryName);

        com.appointment.api.services.dao.Service s = new com.appointment.api.services.dao.Service();
        s.setId(UUID.randomUUID());
        s.setCode(name.toUpperCase().replace(' ', '_'));
        s.setName(name);
        s.setDescription(name + " description");
        s.setCategory(category);
        s.setAvailableToRegistered(registered);
        s.setAvailableToGuest(guest);
        return s;
    }

    @Test
    void getAllActiveServices_groupsByCategoryPreservingOrderAndMapsPageMetadata() {
        Pageable pageable = PageRequest.of(1, 3);
        var a1 = service("Open Account", "Accounts", true, false);
        var l1 = service("Home Loan", "Loans", true, true);
        var a2 = service("Close Account", "Accounts", false, true);
        Page<com.appointment.api.services.dao.Service> page =
                new PageImpl<>(List.of(a1, l1, a2), pageable, 10);
        when(servicesRepository.findActiveServices("", pageable)).thenReturn(page);

        ServiceResponsePage result = servicesImpl.getAllActiveServices(null, pageable);

        assertThat(result.getContent().keySet()).containsExactly("Accounts", "Loans");
        assertThat(result.getContent().get("Accounts"))
                .extracting(ServiceResponse::name)
                .containsExactly("Open Account", "Close Account");
        assertThat(result.getContent().get("Loans")).hasSize(1);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getTotalElements()).isEqualTo(10);
        assertThat(result.getTotalPages()).isEqualTo(4);
        assertThat(result.isFirst()).isFalse();
        assertThat(result.isLast()).isFalse();
    }

    @Test
    void getAllActiveServices_mapsAvailability() {
        Pageable pageable = PageRequest.of(0, 10);
        var s = service("Home Loan", "Loans", true, true);
        when(servicesRepository.findActiveServices("", pageable))
                .thenReturn(new PageImpl<>(List.of(s), pageable, 1));

        ServiceResponsePage result = servicesImpl.getAllActiveServices(null, pageable);

        assertThat(result.getContent().get("Loans").get(0).availableTo())
                .isEqualTo(Set.of(CustomerType.CLIENT, CustomerType.GUEST));
    }

    @Test
    void getAllActiveServices_withNullCriteria_usesEmptyFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        when(servicesRepository.findActiveServices("", pageable)).thenReturn(Page.empty(pageable));

        ServiceResponsePage result = servicesImpl.getAllActiveServices(null, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(servicesRepository).findActiveServices("", pageable);
    }

    @Test
    void getAllActiveServices_withNullSearch_usesEmptyFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        when(servicesRepository.findActiveServices("", pageable)).thenReturn(Page.empty(pageable));

        servicesImpl.getAllActiveServices(new ServiceSearchCriteria(null), pageable);

        verify(servicesRepository).findActiveServices("", pageable);
    }

    @Test
    void getAllActiveServices_trimsSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        when(servicesRepository.findActiveServices("loan", pageable)).thenReturn(Page.empty(pageable));

        servicesImpl.getAllActiveServices(new ServiceSearchCriteria("  loan  "), pageable);

        verify(servicesRepository).findActiveServices("loan", pageable);
    }

    @Test
    void getActiveServiceById_returnsMappedService() {
        var s = service("Open Account", "Accounts", true, false);
        when(servicesRepository.findActiveServiceById(s.getId())).thenReturn(Optional.of(s));

        ServiceResponse result = servicesImpl.getActiveServiceById(s.getId().toString());

        assertThat(result.id()).isEqualTo(s.getId());
        assertThat(result.name()).isEqualTo("Open Account");
        assertThat(result.category()).isEqualTo("Accounts");
        assertThat(result.availableTo()).containsExactly(CustomerType.CLIENT);
    }

    @Test
    void getActiveServiceById_whenMissing_throwsServiceNotFound() {
        UUID id = UUID.randomUUID();
        when(servicesRepository.findActiveServiceById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicesImpl.getActiveServiceById(id.toString()))
                .isInstanceOf(ServiceNotFoundException.class)
                .hasMessage("Active service not found: " + id);
    }

    @Test
    void getActiveServiceById_withInvalidUuid_throwsInvalidArgument() {
        assertThatThrownBy(() -> servicesImpl.getActiveServiceById("not-a-uuid"))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Invalid service ID: not-a-uuid")
                .hasCauseInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(servicesRepository);
    }
}
