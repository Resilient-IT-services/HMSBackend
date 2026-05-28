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
import com.res.hms.iam.domain.DoctorProfile;
import com.res.hms.iam.domain.Role;
import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DoctorAvailabilityServiceUnitTest {

  @Mock private DoctorScheduleRepository scheduleRepository;
  @Mock private DoctorAvailabilityOverrideRepository availabilityOverrideRepository;
  @Mock private AppointmentRepository appointmentRepository;

  @InjectMocks private DoctorAvailabilityService doctorAvailabilityService;

  private UUID hospitalId;
  private DoctorProfile doctorProfile;
  private UserContract assignment;
  private ZoneId zoneId;

  @BeforeEach
  void setUp() {
    hospitalId = UUID.randomUUID();
    UserAccount user = new UserAccount();
    user.setId(UUID.randomUUID());

    doctorProfile = new DoctorProfile();
    doctorProfile.setId(UUID.randomUUID());
    doctorProfile.setUser(user);

    Role drRole = new Role();
    drRole.setCode(RoleCode.DOCTOR);

    assignment = new UserContract();
    assignment.setHospitalId(hospitalId);
    assignment.setRole(drRole);
    assignment.setUser(user);
    user.getContracts().add(assignment);

    zoneId = ZoneId.of("Asia/Kolkata");
  }

  @Test
  void testGetAvailableSlots_FullDayOff() {
    DoctorAvailabilityOverride override = new DoctorAvailabilityOverride();
    override.setType(ExceptionType.FULL_DAY_OFF);
    when(availabilityOverrideRepository.findByDoctorIdAndHospitalIdAndDate(
            eq(doctorProfile.getId()), eq(hospitalId), any()))
        .thenReturn(List.of(override));

    var response =
        doctorAvailabilityService.getAvailableSlots(
            doctorProfile, assignment, LocalDate.now().plusDays(1), zoneId);

    assertTrue(response.getSessions().isEmpty());
  }

  @Test
  void testGetAvailableSlots_RegularSchedule() {
    LocalDate nextMonday = LocalDate.now().plusWeeks(1);
    while (nextMonday.getDayOfWeek() != DayOfWeek.MONDAY) {
      nextMonday = nextMonday.plusDays(1);
    }

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

    var response =
        doctorAvailabilityService.getAvailableSlots(doctorProfile, assignment, nextMonday, zoneId);

    assertFalse(response.getSessions().isEmpty());
    assertEquals(LocalTime.of(9, 0), response.getSessions().get(0).getStartTime());
    assertEquals(10, response.getSessions().get(0).getMaxPatients());
  }
}
