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
import com.res.hms.clinical.domain.DoctorSchedule;
import com.res.hms.clinical.domain.Vitals;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.clinical.infrastructure.repository.DoctorScheduleRepository;
import com.res.hms.core.exception.ApiException;
import com.res.hms.hospital.domain.Hospital;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.UserContractRepository;
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
  private DoctorScheduleRepository doctorScheduleRepository;
  private UserContractRepository userContractRepository;
  private AppointmentValidationHelper validationHelper;

  @BeforeEach
  void setUp() {
    hospitalRepository = mock(HospitalRepository.class);
    appointmentRepository = mock(AppointmentRepository.class);
    doctorScheduleRepository = mock(DoctorScheduleRepository.class);
    userContractRepository = mock(UserContractRepository.class);
    validationHelper =
        new AppointmentValidationHelper(
            hospitalRepository,
            appointmentRepository,
            doctorScheduleRepository,
            userContractRepository);
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

    UserContract contract = new UserContract();
    contract.setId(UUID.randomUUID());
    when(userContractRepository.findByDoctorIdAndHospitalId(doctorId, hospitalId))
        .thenReturn(Optional.of(contract));

    DoctorSchedule schedule = new DoctorSchedule();
    schedule.setStartTime(LocalTime.of(9, 0));
    schedule.setEndTime(LocalTime.of(11, 0));
    schedule.setMaxPatients(30); // 120 mins / 30 = 4 mins slot. Buffer = 1 min.

    when(doctorScheduleRepository.findByAssignmentAndDay(eq(contract.getId()), any()))
        .thenReturn(List.of(schedule));
    when(appointmentRepository.findByDoctorIdAndDateRange(eq(doctorId), any(), any()))
        .thenReturn(Collections.emptyList());

    validationHelper.validateAppointmentProximity(dto, hospitalId, zoneId, new Instant[2]);
    // Should not throw exception
  }

  @Test
  void testValidateAppointmentProximity_NoAssignment() {
    UUID hospitalId = UUID.randomUUID();
    UUID doctorId = UUID.randomUUID();
    when(userContractRepository.findByDoctorIdAndHospitalId(doctorId, hospitalId))
        .thenReturn(Optional.empty());

    AppointmentDTO dto =
        AppointmentDTO.builder().doctorId(doctorId).appointmentTime(OffsetDateTime.now()).build();
    validationHelper.validateAppointmentProximity(
        dto, hospitalId, ZoneId.of("UTC"), new Instant[2]);
    // Should not throw exception
  }

  @Test
  void testValidateAppointmentProximity_NoSchedule() {
    UUID hospitalId = UUID.randomUUID();
    UUID doctorId = UUID.randomUUID();
    UserContract contract = new UserContract();
    contract.setId(UUID.randomUUID());
    when(userContractRepository.findByDoctorIdAndHospitalId(doctorId, hospitalId))
        .thenReturn(Optional.of(contract));
    when(doctorScheduleRepository.findByAssignmentAndDay(any(), any()))
        .thenReturn(Collections.emptyList());
    when(appointmentRepository.findByDoctorIdAndDateRange(any(), any(), any()))
        .thenReturn(Collections.emptyList());

    AppointmentDTO dto =
        AppointmentDTO.builder().doctorId(doctorId).appointmentTime(OffsetDateTime.now()).build();
    validationHelper.validateAppointmentProximity(
        dto, hospitalId, ZoneId.of("UTC"), new Instant[2]);
    // Should not throw exception (uses default 60s buffer)
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

    UserContract contract = new UserContract();
    contract.setId(UUID.randomUUID());
    when(userContractRepository.findByDoctorIdAndHospitalId(doctorId, hospitalId))
        .thenReturn(Optional.of(contract));

    DoctorSchedule schedule = new DoctorSchedule();
    schedule.setStartTime(LocalTime.of(9, 0));
    schedule.setEndTime(LocalTime.of(11, 0));
    schedule.setMaxPatients(30); // 1 min buffer

    when(doctorScheduleRepository.findByAssignmentAndDay(eq(contract.getId()), any()))
        .thenReturn(List.of(schedule));

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
