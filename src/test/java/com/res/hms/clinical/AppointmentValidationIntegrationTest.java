package com.res.hms.clinical;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.res.hms.TestDataHelper;
import com.res.hms.clinical.application.appointment.dto.AppointmentDTO;
import com.res.hms.clinical.application.appointment.service.AppointmentAggregateService;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class AppointmentValidationIntegrationTest {

  @Autowired private AppointmentAggregateService appointmentAggregateService;
  @Autowired private TestDataHelper testDataHelper;
  @Autowired private com.res.hms.core.tenant.HospitalContext hospitalContext;
  @Autowired private com.res.hms.clinical.infrastructure.repository.AppointmentRepository appointmentRepository;
  @Autowired private com.res.hms.clinical.infrastructure.repository.PatientVisitRepository visitRepository;
  @Autowired private com.res.hms.iam.infrastructure.repository.UserAccountRepository userAccountRepository;
  @Autowired private com.res.hms.iam.infrastructure.repository.DoctorHospitalDetailsRepository doctorHospitalDetailsRepository;

  private UUID hospitalId;
  private UUID patientId;
  private UUID doctorId;

  @BeforeEach
  void setup() {
    hospitalId = testDataHelper.createHospitalIfMissing("VAL_HOSP", "Validation Hospital");
    com.res.hms.core.tenant.TenantContext.setHospitalId(hospitalId);
    patientId = testDataHelper.createTestPatient(hospitalId).getId();
    String username = "val_dr_" + UUID.randomUUID().toString().substring(0, 8);
    doctorId = testDataHelper.createTestDoctor(hospitalId, username);
    testDataHelper.setConsultationFee(doctorId, hospitalId, java.math.BigDecimal.ZERO);
  }

  @org.junit.jupiter.api.AfterEach
  void tearDown() {
    appointmentRepository.deleteAll();
    visitRepository.deleteAll();
    doctorHospitalDetailsRepository.deleteAll();
    userAccountRepository.deleteAll();
  }

  @Test
  void testCreateAppointmentInPastFails() {
    AppointmentDTO request = AppointmentDTO.builder()
        .patientId(patientId)
        .doctorId(doctorId)
        .appointmentTime(OffsetDateTime.now().minusHours(2)) // 2 hours in the past
        .build();

    assertThrows(ApiException.class, () -> {
      appointmentAggregateService.saveAppointmentAggregate(request);
    }, "Should not allow booking an appointment in the past");
  }

  @Test
  void testCreateAppointmentInFutureSucceeds() {
    AppointmentDTO request = AppointmentDTO.builder()
        .patientId(patientId)
        .doctorId(doctorId)
        .appointmentTime(OffsetDateTime.now().plusHours(2)) // 2 hours in the future
        .build();

    appointmentAggregateService.saveAppointmentAggregate(request);
    // Should not throw exception
  }
}
