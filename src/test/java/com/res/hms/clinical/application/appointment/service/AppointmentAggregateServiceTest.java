package com.res.hms.clinical.application.appointment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.res.hms.clinical.application.appointment.dto.AppointmentDTO;
import com.res.hms.clinical.application.appointment.dto.VitalsDTO;
import com.res.hms.clinical.application.patient.service.PatientService;
import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.Patient;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.iam.application.service.DoctorProfileService;
import com.res.hms.iam.domain.DoctorProfile;
import com.res.hms.core.service.DomainEventPublisher;
import com.res.hms.clinical.application.vital.service.VitalValidationService;

@ExtendWith(MockitoExtension.class)
class AppointmentAggregateServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private AppointmentService appointmentService;
    @Mock private PatientService patientService;
    @Mock private DoctorProfileService doctorProfileService;
    @Mock private HospitalContext hospitalContext;
    @Mock private VitalValidationService vitalValidationService;
    @Mock private DomainEventPublisher eventPublisher;

    @InjectMocks
    private AppointmentAggregateService aggregateService;

    private UUID hospitalId;

    @BeforeEach
    void setUp() {
        hospitalId = UUID.randomUUID();
        when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    }

    @Test
    void testSaveNewAppointment() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        
        AppointmentDTO dto = AppointmentDTO.builder()
            .patientId(patientId)
            .doctorId(doctorId)
            .appointmentTime(LocalDateTime.now())
            .build();

        Patient patient = new Patient();
        patient.setId(patientId);
        patient.setFullName("John Doe");
        
        DoctorProfile doctor = new DoctorProfile();
        doctor.setId(doctorId);
        
        when(patientService.requirePatient(patientId, hospitalId)).thenReturn(patient);
        when(doctorProfileService.requireDoctor(doctorId, hospitalId)).thenReturn(doctor);
        when(appointmentRepository.findMaxTokenByDoctorAndDate(any(), any())).thenReturn(null);
        
        aggregateService.saveAppointmentAggregate(dto);

        verify(appointmentRepository).saveAndFlush(any(Appointment.class));
        verify(eventPublisher).publish(eq("appointment"), any(), any());
    }

    @Test
    void testUpdateVitals() {
        UUID apptId = UUID.randomUUID();
        VitalsDTO vitalsDto = VitalsDTO.builder()
            .bloodPressure("120/80")
            .heartRate(72)
            .build();

        Appointment appointment = new Appointment();
        appointment.setId(apptId);
        appointment.setHospitalId(hospitalId);

        when(appointmentRepository.findByIdAndHospitalId(apptId, hospitalId)).thenReturn(Optional.of(appointment));
        
        aggregateService.updateVitals(apptId, vitalsDto);

        assertNotNull(appointment.getVitals());
        assertEquals("120/80", appointment.getVitals().getBloodPressure());
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void testUpdateConsultation() {
        UUID apptId = UUID.randomUUID();
        AppointmentDTO dto = AppointmentDTO.builder()
            .chiefComplaints("Headache")
            .diagnosis("Migraine")
            .build();

        Appointment appointment = new Appointment();
        appointment.setId(apptId);
        appointment.setHospitalId(hospitalId);

        when(appointmentRepository.findByIdAndHospitalId(apptId, hospitalId)).thenReturn(Optional.of(appointment));
        
        aggregateService.updateConsultation(apptId, dto);

        assertEquals("Headache", appointment.getChiefComplaints());
        assertEquals("Migraine", appointment.getDiagnosis());
        assertEquals(com.res.hms.clinical.domain.AppointmentStatus.COMPLETED, appointment.getStatus());
        verify(appointmentRepository).save(appointment);
    }
}
