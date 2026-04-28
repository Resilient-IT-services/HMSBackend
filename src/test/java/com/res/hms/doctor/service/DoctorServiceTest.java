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
import com.res.hms.doctor.repository.DoctorProfileRepository;
import com.res.hms.repository.DoctorHospitalDetailsRepository;
import com.res.hms.repository.UserAccountRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

  @Mock private DoctorProfileRepository doctorRepository;
  @Mock private DoctorHospitalDetailsRepository doctorHospitalDetailsRepository;
  @Mock private UserAccountRepository userAccountRepository;
  @Mock private HospitalContext hospitalContext;
  @Mock private ResourceLookup resourceLookup;

  @InjectMocks private DoctorProfileService doctorService;

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
