package com.res.hms.ipd.application.service;

import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.core.util.ResourceLookup;
import com.res.hms.iam.application.service.AuthService;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import com.res.hms.ipd.application.dto.CreateProgressNoteRequest;
import com.res.hms.ipd.application.dto.ProgressNoteResponse;
import com.res.hms.ipd.application.mapper.IpdMapper;
import com.res.hms.ipd.domain.Admission;
import com.res.hms.ipd.domain.AdmissionStatus;
import com.res.hms.ipd.domain.ProgressNote;
import com.res.hms.ipd.infrastructure.repository.AdmissionRepository;
import com.res.hms.ipd.infrastructure.repository.ProgressNoteRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProgressNoteService {

  private final ProgressNoteRepository progressNoteRepository;
  private final AdmissionRepository admissionRepository;
  private final UserAccountRepository userRepository;
  private final AuthService authService;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;
  private final IpdMapper ipdMapper;

  @Transactional
  public ProgressNoteResponse create(UUID admissionId, CreateProgressNoteRequest dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    UUID doctorId = authService.getAuthenticatedPrincipal().userId();

    // Validate note is not blank
    if (dto == null || dto.getNote() == null || dto.getNote().isBlank()) {
      throw new ApiException("INVALID_NOTE", "Note cannot be empty", HttpStatus.BAD_REQUEST);
    }

    // Validate Admission exists and belongs to current hospital
    Admission admission =
        resourceLookup.findRequired(
            admissionId, hospitalId, admissionRepository::findByIdAndHospitalId, "Admission");

    // Validate Admission status is ADMITTED
    if (admission.getStatus() != AdmissionStatus.ADMITTED) {
      throw new ApiException(
          "INVALID_ADMISSION_STATUS",
          "Admission must be ADMITTED to add progress notes",
          HttpStatus.BAD_REQUEST);
    }

    // Fetch Doctor
    UserAccount doctor =
        userRepository
            .findById(doctorId)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Doctor not found", HttpStatus.NOT_FOUND));

    // Create Progress Note
    ProgressNote progressNote = new ProgressNote();
    progressNote.setHospitalId(hospitalId);
    progressNote.setAdmissionId(admission.getId());
    progressNote.setAdmission(admission);
    progressNote.setDoctorId(doctor.getId());
    progressNote.setDoctor(doctor);
    progressNote.setNote(dto.getNote().trim());

    ProgressNote saved = progressNoteRepository.save(progressNote);
    return ipdMapper.toDto(saved);
  }

  @Transactional(readOnly = true)
  public List<ProgressNoteResponse> findByAdmission(UUID admissionId) {
    UUID hospitalId = hospitalContext.requireHospitalId();

    // Validate Admission exists and belongs to current hospital
    resourceLookup.findRequired(
        admissionId, hospitalId, admissionRepository::findByIdAndHospitalId, "Admission");

    List<ProgressNote> notes =
        progressNoteRepository.findAllByAdmissionIdAndHospitalIdOrderByCreatedAtDesc(
            admissionId, hospitalId);

    return notes.stream().map(ipdMapper::toDto).toList();
  }
}
