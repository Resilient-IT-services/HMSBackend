package com.res.hms.iam.application.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.iam.application.dto.DoctorAvailabilityResponse;
import com.res.hms.iam.application.repository.DoctorProfileRepository;
import com.res.hms.iam.domain.DoctorProfile;
import com.res.hms.iam.domain.Role;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DoctorProfileServiceUnitTest {

  @Mock private DoctorProfileRepository doctorProfileRepository;
  @Mock private HospitalContext hospitalContext;
  @Mock private DoctorAvailabilityService doctorAvailabilityService;

  @Mock
  private com.res.hms.hospital.infrastructure.repository.HospitalRepository hospitalRepository;

  @Mock private com.res.hms.core.util.ResourceLookup resourceLookup;
  @Mock private DoctorHospitalProfileService doctorHospitalProfileService;

  @Mock
  private com.res.hms.finance.infrastructure.repository.InvoiceItemRepository invoiceItemRepository;

  @InjectMocks private DoctorProfileService doctorProfileService;

  private UUID hospitalId;
  private UUID doctorId;
  private DoctorProfile doctorProfile;

  @BeforeEach
  void setUp() {
    hospitalId = UUID.randomUUID();
    doctorId = UUID.randomUUID();
    UserAccount user = new UserAccount();
    user.setId(UUID.randomUUID());
    user.setActive(true);

    doctorProfile = new DoctorProfile();
    doctorProfile.setId(doctorId);
    doctorProfile.setUser(user);

    Role drRole = new Role();
    drRole.setCode(RoleCode.DOCTOR);

    UserContract assignment = new UserContract();
    assignment.setHospitalId(hospitalId);
    assignment.setRole(drRole);
    assignment.setUser(user);
    user.getContracts().add(assignment);
  }

  @Test
  void testGetAvailableSlots_Delegation() {
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    when(doctorProfileRepository.findById(doctorId)).thenReturn(Optional.of(doctorProfile));
    when(doctorAvailabilityService.getAvailableSlots(any(), any(), any(), any()))
        .thenReturn(new DoctorAvailabilityResponse(Collections.emptyList()));

    var response = doctorProfileService.getAvailableSlots(doctorId, LocalDate.now().plusDays(1));

    assertNotNull(response);
    verify(doctorAvailabilityService).getAvailableSlots(any(), any(), any(), any());
  }
}
