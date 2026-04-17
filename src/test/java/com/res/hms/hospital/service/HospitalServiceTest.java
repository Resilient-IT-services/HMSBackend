package com.res.hms.hospital.service;

import com.res.hms.common.exception.ApiException;
import com.res.hms.domain.Hospital;
import com.res.hms.hospital.dto.CreateHospitalRequest;
import com.res.hms.repository.HospitalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HospitalServiceTest {

    @Mock
    private HospitalRepository hospitalRepository;

    @InjectMocks
    private HospitalService hospitalService;

    @Test
    void createRejectsDuplicateCode() {
        when(hospitalRepository.findByCode("HMS")).thenReturn(Optional.of(new Hospital()));

        ApiException exception = assertThrows(ApiException.class, () ->
                hospitalService.create(new CreateHospitalRequest("Res", "HMS", "Kochi"))
        );

        assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
    }

    @Test
    void createPersistsHospital() {
        when(hospitalRepository.findByCode("HMS")).thenReturn(Optional.empty());
        when(hospitalRepository.save(any(Hospital.class))).thenAnswer(invocation -> {
            Hospital hospital = invocation.getArgument(0);
            hospital.setId(1L);
            return hospital;
        });

        var response = hospitalService.create(new CreateHospitalRequest("Res", "HMS", "Kochi"));

        assertEquals(1L, response.id());
        assertEquals("ACTIVE", response.status());
    }

    @Test
    void listMapsHospitals() {
        Hospital hospital = new Hospital();
        hospital.setId(2L);
        hospital.setName("Res");
        hospital.setCode("TEST");
        hospital.setAddress("Calicut");
        hospital.setStatus("ACTIVE");
        when(hospitalRepository.findAll()).thenReturn(List.of(hospital));

        var response = hospitalService.list();

        assertEquals(1, response.size());
        assertEquals("TEST", response.getFirst().code());
    }
}
