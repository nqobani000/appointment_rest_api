package com.appointment.api.services.controller;

import com.appointment.api.customers.CustomerType;
import com.appointment.api.exceptions.GlobalExceptionHandler;
import com.appointment.api.exceptions.InvalidArgumentException;
import com.appointment.api.exceptions.ResourceNotFoundException;
import com.appointment.api.services.dto.ServiceResponse;
import com.appointment.api.services.dto.ServiceResponsePage;
import com.appointment.api.services.dto.ServiceSearchCriteria;
import com.appointment.api.services.service.Services;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ServicesControllerTest {

    @Mock
    private Services servicesService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ServicesController(servicesService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private static ServiceResponse response(UUID id) {
        return new ServiceResponse(id, "OPEN_ACCOUNT", "Open Account", "desc",
                "Accounts", Set.of(CustomerType.CLIENT));
    }

    @Test
    void getAllActiveServices_returnsGroupedPage() throws Exception {
        UUID id = UUID.randomUUID();
        LinkedHashMap<String, List<ServiceResponse>> grouped = new LinkedHashMap<>();
        grouped.put("Accounts", List.of(response(id)));
        when(servicesService.getAllActiveServices(any(), any()))
                .thenReturn(new ServiceResponsePage(grouped, 0, 10, 1, 1, true, true));

        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.Accounts[0].id").value(id.toString()))
                .andExpect(jsonPath("$.content.Accounts[0].name").value("Open Account"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getAllActiveServices_bindsSearchAndPagingParameters() throws Exception {
        when(servicesService.getAllActiveServices(any(), any()))
                .thenReturn(new ServiceResponsePage(new LinkedHashMap<>(), 2, 5, 0, 0, false, true));

        mockMvc.perform(get("/api/services")
                        .param("search", "loan")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<ServiceSearchCriteria> criteria = ArgumentCaptor.forClass(ServiceSearchCriteria.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(servicesService).getAllActiveServices(criteria.capture(), pageable.capture());
        assertThat(criteria.getValue().search()).isEqualTo("loan");
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void getActiveServiceById_returnsService() throws Exception {
        UUID id = UUID.randomUUID();
        when(servicesService.getActiveServiceById(id.toString())).thenReturn(response(id));

        mockMvc.perform(get("/api/services/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value("OPEN_ACCOUNT"))
                .andExpect(jsonPath("$.availableTo[0]").value("CLIENT"));
    }

    @Test
    void getActiveServiceById_whenNotFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(servicesService.getActiveServiceById(id.toString()))
                .thenThrow(new ResourceNotFoundException("Active service", id));

        mockMvc.perform(get("/api/services/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Active service not found: " + id));
    }

    @Test
    void getActiveServiceById_whenServiceRejectsId_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        when(servicesService.getActiveServiceById(id.toString()))
                .thenThrow(new InvalidArgumentException("Invalid service ID"));

        mockMvc.perform(get("/api/services/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid service ID"));
    }

    @Test
    void getActiveServiceById_withMalformedUuid_doesNotReachService() throws Exception {
        mockMvc.perform(get("/api/services/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'serviceId': not-a-uuid"));

        org.mockito.Mockito.verifyNoInteractions(servicesService);
    }

    @Test
    void getAllActiveServicesByBranchId_whenBranchMissing_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(servicesService.getAllActiveServicesByBranchId(org.mockito.ArgumentMatchers.eq(id.toString()),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new ResourceNotFoundException("Branch", id));

        mockMvc.perform(get("/api/services/branch/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Branch not found: " + id));
    }

    @Test
    void getAllActiveServicesByBranchId_withMalformedUuid_returns400() throws Exception {
        mockMvc.perform(get("/api/services/branch/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}
