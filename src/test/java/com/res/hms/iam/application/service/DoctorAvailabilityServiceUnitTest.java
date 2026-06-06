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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DoctorAvailabilityServiceUnitTest {

  @Mock private DoctorScheduleRepository scheduleRepository;
  @Mock private DoctorAvailabilityOverrideRepository availabilityOverrideRepository;
  @Mock private AppointmentRepository appointmentRepository;
  private java.time.Clock clock =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-06-04T10:00:00Z"), java.time.ZoneId.of("UTC"));

  private DoctorAvailabilityService doctorAvailabilityService;

  private UUID hospitalId;
  private DoctorProfile doctorProfile;
  private UserContract assignment;
  private ZoneId zoneId;

  @BeforeEach
  void setUp() {
    doctorAvailabilityService =
        new DoctorAvailabilityService(
            scheduleRepository, availabilityOverrideRepository, appointmentRepository, clock);

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
    when(appointmentRepository.findByDoctorIdAndDateRange(any(), any(), any()))
        .thenReturn(List.of());

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

  @Test
  void testGetAvailableSlots_Today_SuggestedTime() {
    // Clock is fixed at 15:30 IST
    LocalDate today = LocalDate.of(2026, 6, 4);

    when(availabilityOverrideRepository.findByDoctorIdAndHospitalIdAndDate(any(), any(), eq(today)))
        .thenReturn(List.of());
    when(appointmentRepository.findByDoctorIdAndDateRange(any(), any(), any()))
        .thenReturn(List.of());

    DoctorSchedule schedule = new DoctorSchedule();
    schedule.setDayOfWeek(today.getDayOfWeek());
    schedule.setStartTime(LocalTime.of(9, 0));
    schedule.setEndTime(LocalTime.of(18, 0));
    schedule.setMaxPatients(20);
    schedule.setSlotDurationMinutes(15);

    when(scheduleRepository.findByAssignmentId(any())).thenReturn(List.of(schedule));

    var response =
        doctorAvailabilityService.getAvailableSlots(doctorProfile, assignment, today, zoneId);

    assertFalse(response.getSessions().isEmpty());
    // Suggested time should be 15:30 or later, not 09:00
    assertEquals(LocalTime.of(15, 30), response.getSessions().get(0).getNextSuggestedTime());
  }

  @Test
  void testGetAvailableSlots_Today_BookedSlots() {
    // Clock is fixed at 15:30 IST
    LocalDate today = LocalDate.of(2026, 6, 4);

    when(availabilityOverrideRepository.findByDoctorIdAndHospitalIdAndDate(any(), any(), eq(today)))
        .thenReturn(List.of());

    // Mock an appointment at 15:30
    com.res.hms.clinical.domain.Appointment apt = new com.res.hms.clinical.domain.Appointment();
    apt.setAppointmentTime(
        java.time.LocalDateTime.of(today, LocalTime.of(15, 30)).atZone(zoneId).toInstant());
    when(appointmentRepository.findByDoctorIdAndDateRange(any(), any(), any()))
        .thenReturn(List.of(apt));

    DoctorSchedule schedule = new DoctorSchedule();
    schedule.setDayOfWeek(today.getDayOfWeek());
    schedule.setStartTime(LocalTime.of(9, 0));
    schedule.setEndTime(LocalTime.of(18, 0));
    schedule.setMaxPatients(20);
    schedule.setSlotDurationMinutes(15);

    when(scheduleRepository.findByAssignmentId(any())).thenReturn(List.of(schedule));

    var response =
        doctorAvailabilityService.getAvailableSlots(doctorProfile, assignment, today, zoneId);

    assertFalse(response.getSessions().isEmpty());
    // 15:30 is booked, next suggested should be 15:45
    assertEquals(LocalTime.of(15, 45), response.getSessions().get(0).getNextSuggestedTime());
  }

  @Test
  void testGetSlots_Today_SessionExpired() {
    // Clock is fixed at 15:30 IST
    LocalDate today = LocalDate.of(2026, 6, 4);

    when(availabilityOverrideRepository.findByDoctorIdAndHospitalIdAndDate(any(), any(), eq(today)))
        .thenReturn(List.of());
    when(appointmentRepository.findByDoctorIdAndDateRange(any(), any(), any()))
        .thenReturn(List.of());

    DoctorSchedule schedule = new DoctorSchedule();
    schedule.setDayOfWeek(today.getDayOfWeek());
    schedule.setStartTime(LocalTime.of(15, 0)); // Session starts at 15:00
    schedule.setEndTime(LocalTime.of(16, 0)); // Session ends at 16:00
    schedule.setSlotDurationMinutes(15);

    when(scheduleRepository.findByAssignmentId(any())).thenReturn(List.of(schedule));

    var slots = doctorAvailabilityService.getSlots(doctorProfile, assignment, today, zoneId);

    // Slot 15:00 - 15:15: Start time (15:00) is BEFORE now (15:30) -> SESSION_EXPIRED
    // Slot 15:15 - 15:30: Start time (15:15) is BEFORE now (15:30) -> SESSION_EXPIRED
    // Slot 15:30 - 15:45: Start time (15:30) is NOT BEFORE now (15:30) -> AVAILABLE
    // Slot 15:45 - 16:00: Start time (15:45) is NOT BEFORE now (15:30) -> AVAILABLE

    assertEquals(4, slots.size());
    assertEquals("SESSION_EXPIRED", slots.get(0).getStatus());
    assertFalse(slots.get(0).isAvailable());
    assertEquals("SESSION_EXPIRED", slots.get(1).getStatus());
    assertFalse(slots.get(1).isAvailable());
    assertEquals("AVAILABLE", slots.get(2).getStatus());
    assertTrue(slots.get(2).isAvailable());
    assertEquals("AVAILABLE", slots.get(3).getStatus());
    assertTrue(slots.get(3).isAvailable());
  }

  @Test
  void testGetSlots_Today_StartInPast_SessionExpired() {
    // Clock is fixed at 15:31 IST (10:01 UTC)
    java.time.Clock clock31 =
        java.time.Clock.fixed(
            java.time.Instant.parse("2026-06-04T10:01:00Z"), java.time.ZoneId.of("UTC"));
    doctorAvailabilityService =
        new DoctorAvailabilityService(
            scheduleRepository, availabilityOverrideRepository, appointmentRepository, clock31);

    LocalDate today = LocalDate.of(2026, 6, 4);
    when(availabilityOverrideRepository.findByDoctorIdAndHospitalIdAndDate(any(), any(), eq(today)))
        .thenReturn(List.of());
    when(appointmentRepository.findByDoctorIdAndDateRange(any(), any(), any()))
        .thenReturn(List.of());

    DoctorSchedule schedule = new DoctorSchedule();
    schedule.setDayOfWeek(today.getDayOfWeek());
    schedule.setStartTime(LocalTime.of(15, 30));
    schedule.setEndTime(LocalTime.of(16, 00));
    schedule.setSlotDurationMinutes(15);

    when(scheduleRepository.findByAssignmentId(any())).thenReturn(List.of(schedule));

    var slots = doctorAvailabilityService.getSlots(doctorProfile, assignment, today, zoneId);

    // Slot 15:30 - 15:45: Start time (15:30) is BEFORE now (15:31) -> SESSION_EXPIRED
    // Slot 15:45 - 16:00: Start time (15:45) is AFTER now (15:31) -> AVAILABLE
    assertEquals(2, slots.size());
    assertEquals("SESSION_EXPIRED", slots.get(0).getStatus());
    assertEquals("AVAILABLE", slots.get(1).getStatus());
  }

  @Test
  void testGetAvailableSlots_Today_StartInPast_SuggestedTime() {
    // Clock is fixed at 15:31 IST (10:01 UTC)
    java.time.Clock clock31 =
        java.time.Clock.fixed(
            java.time.Instant.parse("2026-06-04T10:01:00Z"), java.time.ZoneId.of("UTC"));
    doctorAvailabilityService =
        new DoctorAvailabilityService(
            scheduleRepository, availabilityOverrideRepository, appointmentRepository, clock31);

    LocalDate today = LocalDate.of(2026, 6, 4);
    when(availabilityOverrideRepository.findByDoctorIdAndHospitalIdAndDate(any(), any(), eq(today)))
        .thenReturn(List.of());
    when(appointmentRepository.findByDoctorIdAndDateRange(any(), any(), any()))
        .thenReturn(List.of());

    DoctorSchedule schedule = new DoctorSchedule();
    schedule.setDayOfWeek(today.getDayOfWeek());
    schedule.setStartTime(LocalTime.of(15, 0));
    schedule.setEndTime(LocalTime.of(16, 0));
    schedule.setSlotDurationMinutes(15);

    when(scheduleRepository.findByAssignmentId(any())).thenReturn(List.of(schedule));

    var response =
        doctorAvailabilityService.getAvailableSlots(doctorProfile, assignment, today, zoneId);

    // Now is 15:31.
    // Slots: 15:00, 15:15, 15:30 (all start in past)
    // Next suggested should be 15:45
    assertEquals(LocalTime.of(15, 45), response.getSessions().get(0).getNextSuggestedTime());
  }
}
