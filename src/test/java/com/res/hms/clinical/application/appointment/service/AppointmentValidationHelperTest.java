package com.res.hms.clinical.application.appointment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.res.hms.clinical.application.appointment.dto.AppointmentDTO;
import com.res.hms.clinical.application.appointment.dto.VitalsDTO;
import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.Vitals;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.core.exception.ApiException;
import com.res.hms.hospital.domain.Hospital;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.iam.application.dto.DoctorAvailabilityResponse;
import com.res.hms.iam.application.service.DoctorProfileService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppointmentValidationHelperTest {

  private HospitalRepository hospitalRepository;
  private AppointmentRepository appointmentRepository;
  private DoctorProfileService doctorProfileService;
  private AppointmentValidationHelper validationHelper;

  @BeforeEach
  void setUp() {
    hospitalRepository = mock(HospitalRepository.class);
    appointmentRepository = mock(AppointmentRepository.class);
    doctorProfileService = mock(DoctorProfileService.class);
    validationHelper =
        new AppointmentValidationHelper(
            appointmentRepository,
            doctorProfileService,
            hospitalRepository);
  }

  @Test
  void testGetHospitalZoneId() {
    UUID hospitalId = UUID.randomUUID();
    Hospital hospital = new Hospital();
    hospital.setTimezone("UTC");
    when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));

    ZoneId zoneId = validationHelper.getHospitalZoneId(hospitalId);
    assertEquals(ZoneId.of("UTC"), zoneId);
  }

  @Test
  void testGetDayRange() {
    LocalDate date = LocalDate.of(2026, 6, 5);
    ZoneId zoneId = ZoneId.of("UTC");
    Instant[] range = validationHelper.getDayRange(date, zoneId);

    assertEquals(Instant.parse("2026-06-05T00:00:00Z"), range[0]);
    assertEquals(Instant.parse("2026-06-06T00:00:00Z"), range[1]);
  }

  @Test
  void testValidateAppointmentProximity_Success() {
    UUID hospitalId = UUID.randomUUID();
    UUID doctorId = UUID.randomUUID();
    ZoneId zoneId = ZoneId.of("UTC");
    OffsetDateTime time = OffsetDateTime.parse("2026-06-05T10:00:00Z");

    AppointmentDTO dto = AppointmentDTO.builder().doctorId(doctorId).appointmentTime(time).build();

    DoctorAvailabilityResponse availability = DoctorAvailabilityResponse.builder()
        .sessions(List.of(new DoctorAvailabilityResponse.SessionInfo(
            UUID.randomUUID(),
            LocalTime.of(9, 0),
            LocalTime.of(11, 0),
            30, 0, true, LocalTime.of(9, 30)
        )))
        .build();

    when(doctorProfileService.getAvailableSlots(eq(doctorId), any(LocalDate.class)))
        .thenReturn(availability);
    when(appointmentRepository.findByDoctorIdAndDateRange(eq(doctorId), any(), any()))
        .thenReturn(Collections.emptyList());

    validationHelper.validateAppointmentProximity(dto, hospitalId, zoneId, new Instant[2]);
    // Should not throw exception
  }

  @Test
  void testValidateAppointmentProximity_NoSessions() {
    UUID hospitalId = UUID.randomUUID();
    UUID doctorId = UUID.randomUUID();
    ZoneId zoneId = ZoneId.of("UTC");
    OffsetDateTime time = OffsetDateTime.now();

    AppointmentDTO dto = AppointmentDTO.builder().doctorId(doctorId).appointmentTime(time).build();

    when(doctorProfileService.getAvailableSlots(eq(doctorId), any(LocalDate.class)))
        .thenReturn(DoctorAvailabilityResponse.builder().sessions(Collections.emptyList()).build());

    validationHelper.validateAppointmentProximity(dto, hospitalId, zoneId, new Instant[2]);
    // Should not throw exception
  }

  @Test
  void testMapVitalsDtoToEntity_Nulls() {
    validationHelper.mapVitalsDtoToEntity(null, new Vitals());
    validationHelper.mapVitalsDtoToEntity(new VitalsDTO(), null);
    // Should not throw exception
  }

  @Test
  void testGenerateNextToken_NoExisting() {
    UUID doctorId = UUID.randomUUID();
    when(appointmentRepository.findMaxTokenByDoctorAndDateRange(any(), any(), any()))
        .thenReturn(null);

    Integer nextToken =
        validationHelper.generateNextToken(doctorId, Instant.now(), ZoneId.of("UTC"));
    assertEquals(1, nextToken);
  }

  @Test
  void testValidateAppointmentProximity_Failure() {
    UUID hospitalId = UUID.randomUUID();
    UUID doctorId = UUID.randomUUID();
    ZoneId zoneId = ZoneId.of("UTC");
    OffsetDateTime time = OffsetDateTime.parse("2026-06-05T10:00:30Z");

    AppointmentDTO dto = AppointmentDTO.builder().doctorId(doctorId).appointmentTime(time).build();

    DoctorAvailabilityResponse availability = DoctorAvailabilityResponse.builder()
        .sessions(List.of(new DoctorAvailabilityResponse.SessionInfo(
            UUID.randomUUID(),
            LocalTime.of(9, 0),
            LocalTime.of(11, 0),
            30, 0, true, LocalTime.of(9, 30)
        )))
        .build();

    when(doctorProfileService.getAvailableSlots(eq(doctorId), any(LocalDate.class)))
        .thenReturn(availability);

    Appointment existing = new Appointment();
    existing.setAppointmentTime(Instant.parse("2026-06-05T10:00:00Z"));

    when(appointmentRepository.findByDoctorIdAndDateRange(eq(doctorId), any(), any()))
        .thenReturn(List.of(existing));

    assertThrows(
        ApiException.class,
        () ->
            validationHelper.validateAppointmentProximity(dto, hospitalId, zoneId, new Instant[2]));
  }

  @Test
  void testMapVitalsDtoToEntity() {
    VitalsDTO dto = new VitalsDTO();
    dto.setHeartRate(72);
    dto.setTemperature(BigDecimal.valueOf(36.6));

    Vitals entity = new Vitals();
    validationHelper.mapVitalsDtoToEntity(dto, entity);

    assertEquals(72, entity.getHeartRate());
    assertEquals(BigDecimal.valueOf(36.6), entity.getTemperature());
  }

  @Test
  void testGenerateNextToken() {
    UUID doctorId = UUID.randomUUID();
    Instant instant = Instant.parse("2026-06-05T10:00:00Z");
    ZoneId zoneId = ZoneId.of("UTC");

    when(appointmentRepository.findMaxTokenByDoctorAndDateRange(eq(doctorId), any(), any()))
        .thenReturn(5);

    Integer nextToken = validationHelper.generateNextToken(doctorId, instant, zoneId);
    assertEquals(6, nextToken);
  }
}
