package com.res.hms.doctor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.doctor.repository.DoctorProfileRepository;
import com.res.hms.domain.DoctorProfile;
import com.res.hms.domain.UserAccount;
import com.res.hms.doctor.repository.DoctorHospitalDetailsRepository;
import com.res.hms.hospital.repository.HospitalRepository;
import com.res.hms.user.dto.UserDetailsDTO;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.res.hms.user.repository.UserAccountRepository;

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

  @Test
  void updateProfileStoresBlankLicenseAsNull() {
    UUID userId = UUID.nameUUIDFromBytes("doctor".getBytes());
    UserAccount user = new UserAccount();
    user.setId(userId);
    UserDetailsDTO.DoctorInfo info =
        UserDetailsDTO.DoctorInfo.builder().licenseNumber("  ").build();

    when(doctorRepository.findByUserId(userId)).thenReturn(Optional.empty());

    doctorService.updateProfile(user, info);

    ArgumentCaptor<DoctorProfile> captor = ArgumentCaptor.forClass(DoctorProfile.class);
    verify(doctorRepository).save(captor.capture());
    assertNull(captor.getValue().getLicenseNumber());
  }
}
