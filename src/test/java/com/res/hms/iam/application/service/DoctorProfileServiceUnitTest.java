package com.res.hms.iam.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.res.hms.clinical.domain.DoctorAvailabilityOverride;
import com.res.hms.clinical.domain.DoctorSchedule;
import com.res.hms.clinical.domain.ExceptionType;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.clinical.infrastructure.repository.DoctorAvailabilityOverrideRepository;
import com.res.hms.clinical.infrastructure.repository.DoctorScheduleRepository;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.iam.application.repository.DoctorProfileRepository;
import com.res.hms.iam.domain.DoctorProfile;
import com.res.hms.iam.domain.Role;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.UserContractRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
  @Mock private UserContractRepository assignmentRepository;
  @Mock private DoctorAvailabilityOverrideRepository availabilityOverrideRepository;
  @Mock private DoctorScheduleRepository scheduleRepository;
  @Mock private AppointmentRepository appointmentRepository;

  @Mock
  private com.res.hms.hospital.infrastructure.repository.HospitalRepository hospitalRepository;

  @Mock
  private com.res.hms.iam.infrastructure.repository.DoctorHospitalDetailsRepository
      doctorHospitalDetailsRepository;

  @Mock private com.res.hms.core.util.ResourceLookup resourceLookup;

  @Mock
  private com.res.hms.finance.infrastructure.repository.InvoiceItemRepository invoiceItemRepository;

  @InjectMocks private DoctorProfileService doctorProfileService;

  private UUID hospitalId;
  private UUID doctorId;
  private DoctorProfile doctorProfile;
  private UserAccount user;

  @BeforeEach
  void setUp() {
    hospitalId = UUID.randomUUID();
    doctorId = UUID.randomUUID();
    user = new UserAccount();
    user.setId(UUID.randomUUID());

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
  void testGetAvailableSlots_FullDayOff() {
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    when(doctorProfileRepository.findById(doctorId)).thenReturn(Optional.of(doctorProfile));

    DoctorAvailabilityOverride override = new DoctorAvailabilityOverride();
    override.setType(ExceptionType.FULL_DAY_OFF);
    when(availabilityOverrideRepository.findByDoctorIdAndHospitalIdAndDate(
            any(), eq(hospitalId), any()))
        .thenReturn(List.of(override));

    var response = doctorProfileService.getAvailableSlots(doctorId, LocalDate.now().plusDays(1));

    assertTrue(response.getSessions().isEmpty());
  }

  @Test
  void testGetAvailableSlots_RegularSchedule() {
    LocalDate nextMonday = LocalDate.now().plusWeeks(1);
    if (nextMonday.getDayOfWeek().getValue() != 1) {
      nextMonday = nextMonday.plusDays(8 - nextMonday.getDayOfWeek().getValue());
    }

    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    when(doctorProfileRepository.findById(doctorId)).thenReturn(Optional.of(doctorProfile));
    when(availabilityOverrideRepository.findByDoctorIdAndHospitalIdAndDate(any(), any(), any()))
        .thenReturn(List.of());
    when(appointmentRepository.findByDoctorIdAndDate(any(), any())).thenReturn(List.of());

    DoctorSchedule schedule = new DoctorSchedule();
    schedule.setDayOfWeek(DayOfWeek.MONDAY);
    schedule.setStartTime(LocalTime.of(9, 0));
    schedule.setEndTime(LocalTime.of(12, 0));
    schedule.setMaxPatients(10);
    schedule.setSlotDurationMinutes(20);

    when(scheduleRepository.findByAssignmentId(any())).thenReturn(List.of(schedule));

    var response = doctorProfileService.getAvailableSlots(doctorId, nextMonday);

    assertFalse(response.getSessions().isEmpty());
    assertEquals(LocalTime.of(9, 0), response.getSessions().get(0).getStartTime());
    assertEquals(10, response.getSessions().get(0).getMaxPatients());
  }
}
