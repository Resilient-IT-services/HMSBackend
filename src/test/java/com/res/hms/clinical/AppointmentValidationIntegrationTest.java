package com.res.hms.clinical;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.res.hms.TestDataHelper;
import com.res.hms.clinical.application.appointment.dto.AppointmentDTO;
import com.res.hms.clinical.application.appointment.service.AppointmentAggregateService;
import com.res.hms.clinical.domain.AppointmentStatus;
import com.res.hms.core.exception.ApiException;
import com.res.hms.iam.application.repository.DoctorProfileRepository;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AppointmentValidationIntegrationTest {

  @Autowired private AppointmentAggregateService appointmentAggregateService;
  @Autowired private TestDataHelper testDataHelper;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private DoctorProfileRepository doctorProfileRepository;

  @Autowired
  private com.res.hms.clinical.infrastructure.repository.DoctorScheduleRepository
      scheduleRepository;

  private UUID hospitalId;
  private UUID patientId;
  private UUID doctorId;
  private final ZoneId hospitalZone = ZoneId.of("Asia/Kolkata");

  @BeforeEach
  void setup() {
    hospitalId = testDataHelper.createHospitalIfMissing("VAL_HOSP", "Validation Hospital");
    com.res.hms.core.tenant.TenantContext.setHospitalId(hospitalId);
    patientId = testDataHelper.createTestPatient(hospitalId).getId();
    String username = "val_dr_" + UUID.randomUUID().toString().substring(0, 8);
    doctorId = testDataHelper.createTestDoctor(hospitalId, username);
    testDataHelper.setConsultationFee(doctorId, hospitalId, java.math.BigDecimal.ZERO);
  }

  @Test
  void testCreateAppointmentInPastFails() {
    // 2 hours in the past relative to now in hospital zone
    OffsetDateTime pastTime = ZonedDateTime.now(hospitalZone).minusHours(2).toOffsetDateTime();
    AppointmentDTO request =
        AppointmentDTO.builder()
            .patientId(patientId)
            .doctorId(doctorId)
            .appointmentTime(pastTime)
            .build();

    assertThrows(
        ApiException.class,
        () -> {
          appointmentAggregateService.saveAppointmentAggregate(request);
        },
        "Should not allow booking an appointment in the past");
  }

  @Test
  void testCreateAppointmentInFutureSucceeds() {
    // 2 hours in the future relative to now in hospital zone
    OffsetDateTime futureTime = ZonedDateTime.now(hospitalZone).plusHours(2).toOffsetDateTime();
    AppointmentDTO request =
        AppointmentDTO.builder()
            .patientId(patientId)
            .doctorId(doctorId)
            .appointmentTime(futureTime)
            .build();

    appointmentAggregateService.saveAppointmentAggregate(request);
    // Should not throw exception
  }

  @Test
  void testCreateAppointmentTooCloseFails() {
    // Setup schedule: 120 mins (2h), 30 patients => 4 mins avg slot. 25% buffer = 1 min.
    // Session: 09:00 to 11:00
    // Requested: 10:00:00
    // Existing: 10:00:30 (should fail as it is within [09:59, 10:01])

    UserAccount drUser =
        userAccountRepository
            .findById(doctorProfileRepository.findById(doctorId).get().getUser().getId())
            .get();
    UUID assignmentId = drUser.getContracts().stream().findFirst().get().getId();

    ZonedDateTime tomorrow = ZonedDateTime.now(hospitalZone).plusDays(1);

    com.res.hms.clinical.domain.DoctorSchedule schedule =
        new com.res.hms.clinical.domain.DoctorSchedule();
    schedule.setAssignmentId(assignmentId);
    schedule.setDayOfWeek(tomorrow.getDayOfWeek());
    schedule.setStartTime(java.time.LocalTime.of(9, 0));
    schedule.setEndTime(java.time.LocalTime.of(11, 0));
    schedule.setMaxPatients(30);
    scheduleRepository.saveAndFlush(schedule);

    OffsetDateTime time1 =
        tomorrow.withHour(10).withMinute(0).withSecond(0).withNano(0).toOffsetDateTime();

    AppointmentDTO appt1 =
        AppointmentDTO.builder()
            .patientId(patientId)
            .doctorId(doctorId)
            .appointmentTime(time1)
            .status(AppointmentStatus.SCHEDULED)
            .build();
    appointmentAggregateService.saveAppointmentAggregate(appt1);

    // Create another patient
    UUID patient2 = testDataHelper.createTestPatient(hospitalId).getId();

    // Try to book at 10:00:30 (within 1 min buffer of 10:00:00)
    OffsetDateTime time2 = time1.plusSeconds(30);
    AppointmentDTO appt2 =
        AppointmentDTO.builder()
            .patientId(patient2)
            .doctorId(doctorId)
            .appointmentTime(time2)
            .status(AppointmentStatus.SCHEDULED)
            .build();

    assertThrows(
        ApiException.class,
        () -> {
          appointmentAggregateService.saveAppointmentAggregate(appt2);
        },
        "Should fail because 10:00:30 is too close to 10:00:00 (4 min slot needs 1 min buffer)");

    // Try to book at 10:01:30 (outside 1 min buffer of 10:00:00)
    OffsetDateTime time3 = time1.plusMinutes(1).plusSeconds(30);
    AppointmentDTO appt3 =
        AppointmentDTO.builder()
            .patientId(patient2)
            .doctorId(doctorId)
            .appointmentTime(time3)
            .status(AppointmentStatus.SCHEDULED)
            .build();

    appointmentAggregateService.saveAppointmentAggregate(appt3);
    // Should succeed
  }

  @Test
  void testCreateAppointmentProximityExhaustive() {
    // Setup schedule: 60 mins, 10 patients => 6 mins avg slot. 25% buffer = 90 seconds (1.5 min).
    UserAccount drUser =
        userAccountRepository
            .findById(doctorProfileRepository.findById(doctorId).get().getUser().getId())
            .get();
    UUID assignmentId = drUser.getContracts().stream().findFirst().get().getId();

    ZonedDateTime tomorrow = ZonedDateTime.now(hospitalZone).plusDays(1);

    com.res.hms.clinical.domain.DoctorSchedule schedule =
        new com.res.hms.clinical.domain.DoctorSchedule();
    schedule.setAssignmentId(assignmentId);
    schedule.setDayOfWeek(tomorrow.getDayOfWeek());
    schedule.setStartTime(java.time.LocalTime.of(14, 0));
    schedule.setEndTime(java.time.LocalTime.of(15, 0));
    schedule.setMaxPatients(10);
    scheduleRepository.saveAndFlush(schedule);

    ZonedDateTime anchorZdt = tomorrow.withHour(14).withMinute(30).withSecond(0).withNano(0);
    OffsetDateTime anchor = anchorZdt.toOffsetDateTime();

    // Book anchor appointment
    appointmentAggregateService.saveAppointmentAggregate(
        AppointmentDTO.builder()
            .patientId(patientId)
            .doctorId(doctorId)
            .appointmentTime(anchor)
            .status(AppointmentStatus.SCHEDULED)
            .build());

    UUID otherPatient = testDataHelper.createTestPatient(hospitalId).getId();

    // 1. Exact same time -> Fail
    assertThrows(
        ApiException.class,
        () ->
            appointmentAggregateService.saveAppointmentAggregate(
                AppointmentDTO.builder()
                    .patientId(otherPatient)
                    .doctorId(doctorId)
                    .appointmentTime(anchor)
                    .build()));

    // 2. 60 seconds after (within 90s buffer) -> Fail
    assertThrows(
        ApiException.class,
        () ->
            appointmentAggregateService.saveAppointmentAggregate(
                AppointmentDTO.builder()
                    .patientId(otherPatient)
                    .doctorId(doctorId)
                    .appointmentTime(anchor.plusSeconds(60))
                    .build()));

    // 3. 60 seconds before (within 90s buffer) -> Fail
    assertThrows(
        ApiException.class,
        () ->
            appointmentAggregateService.saveAppointmentAggregate(
                AppointmentDTO.builder()
                    .patientId(otherPatient)
                    .doctorId(doctorId)
                    .appointmentTime(anchor.minusSeconds(60))
                    .build()));

    // 4. 100 seconds after (outside 90s buffer) -> Pass
    appointmentAggregateService.saveAppointmentAggregate(
        AppointmentDTO.builder()
            .patientId(otherPatient)
            .doctorId(doctorId)
            .appointmentTime(anchor.plusSeconds(100))
            .status(AppointmentStatus.SCHEDULED)
            .build());

    // 5. 100 seconds before (outside 90s buffer) -> Pass
    UUID patient3 = testDataHelper.createTestPatient(hospitalId).getId();
    appointmentAggregateService.saveAppointmentAggregate(
        AppointmentDTO.builder()
            .patientId(patient3)
            .doctorId(doctorId)
            .appointmentTime(anchor.minusSeconds(100))
            .status(AppointmentStatus.SCHEDULED)
            .build());
  }

  @Test
  void testProximityCheckIsDoctorSpecific() {
    // Two doctors, same time. Should pass.
    ZonedDateTime tomorrow = ZonedDateTime.now(hospitalZone).plusDays(1);
    OffsetDateTime time = tomorrow.withHour(10).withMinute(0).toOffsetDateTime();

    // Doctor 1 booking
    appointmentAggregateService.saveAppointmentAggregate(
        AppointmentDTO.builder()
            .patientId(patientId)
            .doctorId(doctorId)
            .appointmentTime(time)
            .status(AppointmentStatus.SCHEDULED)
            .build());

    // Doctor 2 setup and booking
    UUID doctor2Id = testDataHelper.createTestDoctor(hospitalId, "dr_two");
    testDataHelper.setConsultationFee(doctor2Id, hospitalId, java.math.BigDecimal.ZERO);

    appointmentAggregateService.saveAppointmentAggregate(
        AppointmentDTO.builder()
            .patientId(patientId)
            .doctorId(doctor2Id)
            .appointmentTime(time)
            .status(AppointmentStatus.SCHEDULED)
            .build());

    // Should succeed because they are different doctors
  }

  @Test
  void testCreateAppointmentProximityFallback() {
    // Setup schedule: 60 mins, but NO maxPatients. Should fallback to 60s buffer.
    UserAccount drUser =
        userAccountRepository
            .findById(doctorProfileRepository.findById(doctorId).get().getUser().getId())
            .get();
    UUID assignmentId = drUser.getContracts().stream().findFirst().get().getId();

    ZonedDateTime tomorrow = ZonedDateTime.now(hospitalZone).plusDays(1);

    com.res.hms.clinical.domain.DoctorSchedule schedule =
        new com.res.hms.clinical.domain.DoctorSchedule();
    schedule.setAssignmentId(assignmentId);
    schedule.setDayOfWeek(tomorrow.getDayOfWeek());
    schedule.setStartTime(java.time.LocalTime.of(16, 0));
    schedule.setEndTime(java.time.LocalTime.of(17, 0));
    schedule.setMaxPatients(null); // No patient limit entered
    scheduleRepository.saveAndFlush(schedule);

    ZonedDateTime anchorZdt = tomorrow.withHour(16).withMinute(30).withSecond(0).withNano(0);
    OffsetDateTime anchor = anchorZdt.toOffsetDateTime();

    // Book anchor appointment
    appointmentAggregateService.saveAppointmentAggregate(
        AppointmentDTO.builder()
            .patientId(patientId)
            .doctorId(doctorId)
            .appointmentTime(anchor)
            .status(AppointmentStatus.SCHEDULED)
            .build());

    UUID otherPatient = testDataHelper.createTestPatient(hospitalId).getId();

    // 1. 30 seconds after (within 60s fallback buffer) -> Fail
    assertThrows(
        ApiException.class,
        () ->
            appointmentAggregateService.saveAppointmentAggregate(
                AppointmentDTO.builder()
                    .patientId(otherPatient)
                    .doctorId(doctorId)
                    .appointmentTime(anchor.plusSeconds(30))
                    .build()));

    // 2. 70 seconds after (outside 60s fallback buffer) -> Pass
    appointmentAggregateService.saveAppointmentAggregate(
        AppointmentDTO.builder()
            .patientId(otherPatient)
            .doctorId(doctorId)
            .appointmentTime(anchor.plusSeconds(70))
            .status(AppointmentStatus.SCHEDULED)
            .build());
  }
}
