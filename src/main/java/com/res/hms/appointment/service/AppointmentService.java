package com.res.hms.appointment.service;

import com.res.hms.appointment.dto.AppointmentResponse;
import com.res.hms.appointment.dto.CreateAppointmentRequest;
import com.res.hms.appointment.dto.UpdateAppointmentStatusRequest;
import com.res.hms.appointment.repository.AppointmentRepository;
import com.res.hms.common.api.PageResponse;
import com.res.hms.common.audit.Auditable;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.doctor.service.DoctorService;
import com.res.hms.domain.Appointment;
import com.res.hms.domain.AppointmentStatus;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.Patient;
import com.res.hms.patient.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentService {

  private final AppointmentRepository appointmentRepository;
  private final PatientService patientService;
  private final DoctorService doctorService;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;

  @Transactional
  @Auditable(entity = "appointment", action = "CREATE")
  public AppointmentResponse create(CreateAppointmentRequest request) {
    Long hospitalId = hospitalContext.requireHospitalId();
    Patient patient = patientService.requirePatient(request.patientId(), hospitalId);
    Doctor doctor = doctorService.requireDoctor(request.doctorId(), hospitalId);

    Appointment appointment = new Appointment();
    appointment.setHospitalId(hospitalId);
    appointment.setPatient(patient);
    appointment.setDoctor(doctor);
    appointment.setAppointmentTime(request.appointmentTime());
    appointment.setStatus(AppointmentStatus.SCHEDULED);
    appointment.setNotes(request.notes());
    appointmentRepository.save(appointment);

    return toResponse(appointment);
  }

  @Transactional(readOnly = true)
  public PageResponse<AppointmentResponse> list(int page, int size) {
    Long hospitalId = hospitalContext.requireHospitalId();
    return PageResponse.from(
        appointmentRepository
            .findAllByHospitalIdOrderByAppointmentTimeDesc(hospitalId, PageRequest.of(page, size))
            .map(this::toResponse));
  }

  @Transactional
  @Auditable(entity = "appointment", action = "UPDATE_STATUS")
  public AppointmentResponse updateStatus(Long id, UpdateAppointmentStatusRequest request) {
    Long hospitalId = hospitalContext.requireHospitalId();
    Appointment appointment =
        resourceLookup.findRequired(
            id, hospitalId, appointmentRepository::findByIdAndHospitalId, "Appointment");

    appointment.setStatus(request.status());
    return toResponse(appointmentRepository.save(appointment));
  }

  private AppointmentResponse toResponse(Appointment appointment) {
    return new AppointmentResponse(
        appointment.getId(),
        appointment.getPatient().getId(),
        appointment.getPatient().getFullName(),
        appointment.getDoctor().getId(),
        appointment.getDoctor().getFullName(),
        appointment.getAppointmentTime(),
        appointment.getStatus(),
        appointment.getNotes(),
        appointment.getCreatedAt());
  }
}
