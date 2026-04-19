package com.res.hms.appointment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.res.hms.appointment.dto.CreateAppointmentRequest;
import com.res.hms.appointment.dto.UpdateAppointmentStatusRequest;
import com.res.hms.appointment.repository.AppointmentRepository;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.doctor.service.DoctorService;
import com.res.hms.domain.Appointment;
import com.res.hms.domain.AppointmentStatus;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.Patient;
import com.res.hms.domain.UserAccount;
import com.res.hms.patient.service.PatientService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

  @Mock private AppointmentRepository appointmentRepository;
  @Mock private PatientService patientService;
  @Mock private DoctorService doctorService;
  @Mock private HospitalContext hospitalContext;

  @InjectMocks private AppointmentService appointmentService;

  @Test
  void createPersistsAppointment() {
    Patient patient = new Patient();
    patient.setId(10L);
    patient.setFullName("Asha");

    UserAccount user = new UserAccount();
    user.setFullName("Dr. Maya");
    Doctor doctor = new Doctor();
    doctor.setId(11L);
    doctor.setUser(user);

    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(patientService.requirePatient(10L, 4L)).thenReturn(patient);
    when(doctorService.requireDoctor(11L, 4L)).thenReturn(doctor);
    when(appointmentRepository.save(any(Appointment.class)))
        .thenAnswer(
            invocation -> {
              Appointment appointment = invocation.getArgument(0);
              appointment.setId(50L);
              return appointment;
            });

    var response =
        appointmentService.create(
            new CreateAppointmentRequest(10L, 11L, LocalDateTime.now().plusDays(1), "Follow-up"));

    assertEquals(AppointmentStatus.SCHEDULED, response.status());
  }

  @Test
  void listReturnsMappedAppointments() {
    Patient patient = new Patient();
    patient.setId(10L);
    patient.setFullName("Asha");

    UserAccount user = new UserAccount();
    user.setFullName("Dr. Maya");
    Doctor doctor = new Doctor();
    doctor.setId(11L);
    doctor.setUser(user);

    Appointment appointment = new Appointment();
    appointment.setId(50L);
    appointment.setPatient(patient);
    appointment.setDoctor(doctor);
    appointment.setAppointmentTime(LocalDateTime.now().plusDays(1));
    appointment.setStatus(AppointmentStatus.SCHEDULED);
    appointment.setNotes("Follow-up");
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(appointmentRepository.findAllByHospitalIdOrderByAppointmentTimeDesc(
            eq(4L), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(appointment)));

    var response = appointmentService.list(0, 10);

    assertEquals(1, response.content().size());
    assertEquals("Dr. Maya", response.content().getFirst().doctorName());
  }

  @Test
  void updateStatusRejectsMissingAppointment() {
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(appointmentRepository.findByIdAndHospitalId(99L, 4L)).thenReturn(Optional.empty());

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                appointmentService.updateStatus(
                    99L, new UpdateAppointmentStatusRequest(AppointmentStatus.COMPLETED)));

    assertEquals("NOT_FOUND", exception.getCode());
  }

  @Test
  void updateStatusUpdatesAppointment() {
    Patient patient = new Patient();
    patient.setId(10L);
    patient.setFullName("Asha");

    UserAccount user = new UserAccount();
    user.setFullName("Dr. Maya");
    Doctor doctor = new Doctor();
    doctor.setId(11L);
    doctor.setUser(user);

    Appointment appointment = new Appointment();
    appointment.setId(50L);
    appointment.setPatient(patient);
    appointment.setDoctor(doctor);
    appointment.setAppointmentTime(LocalDateTime.now().plusDays(1));
    appointment.setStatus(AppointmentStatus.SCHEDULED);
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(appointmentRepository.findByIdAndHospitalId(50L, 4L)).thenReturn(Optional.of(appointment));

    var response =
        appointmentService.updateStatus(
            50L, new UpdateAppointmentStatusRequest(AppointmentStatus.COMPLETED));

    assertEquals(AppointmentStatus.COMPLETED, response.status());
  }
}
