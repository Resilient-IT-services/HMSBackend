package com.res.hms.hospital.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.res.hms.common.exception.ApiException;
import com.res.hms.domain.Hospital;
import com.res.hms.domain.HospitalStatus;
import com.res.hms.hospital.dto.CreateHospitalRequest;
import com.res.hms.repository.HospitalRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class HospitalServiceTest {

  @Mock private HospitalRepository hospitalRepository;

  @InjectMocks private HospitalService hospitalService;

  @Test
  void createRejectsDuplicateCode() {
    when(hospitalRepository.existsByCode("HMS")).thenReturn(true);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> hospitalService.create(new CreateHospitalRequest("Res", "HMS", "Kochi", null)));

    assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
  }

  @Test
  void createPersistsHospital() {
    when(hospitalRepository.existsByCode("HMS")).thenReturn(false);
    when(hospitalRepository.save(any(Hospital.class)))
        .thenAnswer(
            invocation -> {
              Hospital hospital = invocation.getArgument(0);
              hospital.setId(UUID.nameUUIDFromBytes("1".getBytes()));
              return hospital;
            });

    var response = hospitalService.create(new CreateHospitalRequest("Res", "HMS", "Kochi", null));

    assertEquals(UUID.nameUUIDFromBytes("1".getBytes()), response.id());
    assertEquals(HospitalStatus.ACTIVE, response.status());
  }

  @Test
  void listMapsHospitals() {
    Hospital hospital = new Hospital();
    hospital.setId(UUID.nameUUIDFromBytes("2".getBytes()));
    hospital.setName("Res");
    hospital.setCode("TEST");
    hospital.setAddress("Calicut");
    hospital.setStatus(HospitalStatus.ACTIVE);

    when(hospitalRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(hospital)));

    var response = hospitalService.list(0, 10, null, null);

    assertEquals(1, response.totalElements());
    assertEquals("TEST", response.content().getFirst().code());
  }

  @Test
  void updateUpdatesHospitalDetails() {
    Hospital hospital = new Hospital();
    hospital.setId(UUID.nameUUIDFromBytes("1".getBytes()));
    hospital.setName("Old Name");
    hospital.setCode("OLD");

    when(hospitalRepository.findById(UUID.nameUUIDFromBytes("1".getBytes())))
        .thenReturn(Optional.of(hospital));
    when(hospitalRepository.existsByCodeAndIdNot("NEW", UUID.nameUUIDFromBytes("1".getBytes())))
        .thenReturn(false);
    when(hospitalRepository.save(any(Hospital.class))).thenAnswer(i -> i.getArgument(0));

    var response =
        hospitalService.update(
            UUID.nameUUIDFromBytes("1".getBytes()),
            new CreateHospitalRequest("New Name", "NEW", "New Addr", null));

    assertEquals("New Name", response.name());
    assertEquals("NEW", response.code());
    assertEquals("New Addr", response.address());
  }
}
