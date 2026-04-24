package com.res.hms.doctor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.doctor.repository.DoctorRepository;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.UserAccount;
import com.res.hms.repository.DoctorHospitalDetailsRepository;
import com.res.hms.repository.UserAccountRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

  @Mock private DoctorRepository doctorRepository;
  @Mock private DoctorHospitalDetailsRepository doctorHospitalDetailsRepository;
  @Mock private UserAccountRepository userAccountRepository;
  @Mock private HospitalContext hospitalContext;
  @Mock private ResourceLookup resourceLookup;

  @InjectMocks private DoctorService doctorService;

  @Test
  void listReturnsMappedDoctors() {
    UserAccount user = new UserAccount();
    user.setFullName("Dr. Maya");

    Doctor doctor = new Doctor();
    doctor.setId(UUID.nameUUIDFromBytes("3".getBytes()));
    doctor.setUser(user);
    doctor.setSpecialization("Cardiology");

    UUID hospitalId = UUID.nameUUIDFromBytes("4".getBytes());
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    when(hospitalContext.getHospitalIdOrNull()).thenReturn(hospitalId);
    when(doctorRepository.findAllByHospitalIdOrderByCreatedAtDesc(
            eq(hospitalId), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(doctor)));
    when(doctorHospitalDetailsRepository.findByUserIdAndHospitalId(any(), eq(hospitalId)))
        .thenReturn(Optional.empty());

    var response = doctorService.list(0, 10);

    assertEquals(1, response.content().size());
    assertEquals("Cardiology", response.content().getFirst().specialization());
  }

  @Test
  void requireDoctorRejectsMissingDoctor() {
    UUID doctorId = UUID.nameUUIDFromBytes("12".getBytes());
    UUID hospitalId = UUID.nameUUIDFromBytes("4".getBytes());

    when(resourceLookup.findRequired(eq(doctorId), eq(hospitalId), any(), anyString()))
        .thenThrow(new ApiException("NOT_FOUND", "Doctor not found", HttpStatus.NOT_FOUND));

    ApiException exception =
        assertThrows(ApiException.class, () -> doctorService.requireDoctor(doctorId, hospitalId));

    assertEquals("NOT_FOUND", exception.getCode());
  }
}
