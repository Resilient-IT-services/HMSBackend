package com.res.hms.clinical.application.appointment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.res.hms.clinical.application.appointment.dto.AppointmentDTO;
import com.res.hms.clinical.application.appointment.dto.VitalsDTO;
import com.res.hms.clinical.application.patient.service.PatientService;
import com.res.hms.clinical.application.prescription.service.PrescriptionService;
import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.AppointmentStatus;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.core.util.ResourceLookup;
import com.res.hms.iam.application.service.DoctorProfileService;
import com.res.hms.lab.application.service.LabOrderService;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentAggregateServiceTest {

  @Mock private AppointmentRepository appointmentRepository;
  @Mock private AppointmentService appointmentService;
  @Mock private PrescriptionService prescriptionService;
  @Mock private HospitalContext hospitalContext;
  @Mock private ResourceLookup resourceLookup;
  @Mock private AppointmentValidationHelper validationHelper;
  @Mock private DoctorProfileService doctorProfileService;
  @Mock private LabOrderService labOrderService;
  @Mock private PatientService patientService;
  @Mock private com.res.hms.clinical.application.service.PatientVisitService patientVisitService;

  @Mock
  private com.res.hms.hospital.infrastructure.repository.HospitalRepository hospitalRepository;

  @Mock private com.res.hms.core.service.DomainEventPublisher eventPublisher;

  @Mock
  private com.res.hms.clinical.application.vital.service.VitalValidationService
      vitalValidationService;

  @Mock
  private com.res.hms.clinical.application.service.PatientVisitService patientVisitServiceMock;

  @InjectMocks private AppointmentAggregateService aggregateService;

  private UUID hospitalId;
  private UUID doctorId;
  private UUID appointmentId;

  @BeforeEach
  void setUp() {
    hospitalId = UUID.randomUUID();
    doctorId = UUID.randomUUID();
    appointmentId = UUID.randomUUID();
  }

  @Test
  void testRecordVitals() {
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    Appointment appointment = new Appointment();
    appointment.setId(appointmentId);
    appointment.setStatus(AppointmentStatus.PAID);

    when(appointmentRepository.findByIdAndHospitalId(appointmentId, hospitalId))
        .thenReturn(Optional.of(appointment));

    VitalsDTO vitalsDto =
        VitalsDTO.builder()
            .temperature(java.math.BigDecimal.valueOf(36.6))
            .weight(java.math.BigDecimal.valueOf(70.0))
            .build();

    when(appointmentRepository.save(any())).thenReturn(appointment);
    when(appointmentService.toDto(any())).thenReturn(new AppointmentDTO());

    AppointmentDTO result = aggregateService.updateVitals(appointmentId, vitalsDto);

    assertNotNull(result);
    assertEquals(AppointmentStatus.VITALS_RECORDED, appointment.getStatus());
  }

  @Test
  void testStartNextConsultation() {
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    ZoneId zoneId = ZoneId.of("UTC");
    when(validationHelper.getHospitalZoneId(hospitalId)).thenReturn(zoneId);
    when(validationHelper.getDayRange(any(), any()))
        .thenReturn(new Instant[] {Instant.now(), Instant.now().plusSeconds(1)});

    when(appointmentRepository.findRunningConsultationsForUpdateByDateRange(
            any(), any(), any(), any()))
        .thenReturn(java.util.Collections.emptyList());

    Appointment next = new Appointment();
    next.setId(appointmentId);
    when(appointmentRepository.findWaitingAppointmentsForUpdateByDateRange(
            any(), any(), any(), any(), any()))
        .thenReturn(java.util.List.of(next));

    when(appointmentRepository.saveAndFlush(any())).thenReturn(next);
    when(appointmentService.toDto(any())).thenReturn(new AppointmentDTO());

    AppointmentDTO result = aggregateService.startNextConsultation(doctorId);

    assertNotNull(result);
    assertEquals(AppointmentStatus.CONSULTATION_STARTED, next.getStatus());
  }

  @Test
  void testStartNextConsultationNoPatients() {
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    ZoneId zoneId = ZoneId.of("UTC");
    when(validationHelper.getHospitalZoneId(hospitalId)).thenReturn(zoneId);
    when(validationHelper.getDayRange(any(), any()))
        .thenReturn(new Instant[] {Instant.now(), Instant.now().plusSeconds(1)});

    when(appointmentRepository.findRunningConsultationsForUpdateByDateRange(
            any(), any(), any(), any()))
        .thenReturn(java.util.Collections.emptyList());
    when(appointmentRepository.findWaitingAppointmentsForUpdateByDateRange(
            any(), any(), any(), any(), any()))
        .thenReturn(java.util.Collections.emptyList());

    assertThrows(ApiException.class, () -> aggregateService.startNextConsultation(doctorId));
  }
}
