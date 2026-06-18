package com.res.hms.ipd.application.service;

import com.res.hms.clinical.domain.Patient;
import com.res.hms.clinical.infrastructure.repository.PatientRepository;
import com.res.hms.core.api.PageResponse;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.core.util.ResourceLookup;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import com.res.hms.ipd.application.dto.AdmissionRequestDTO;
import com.res.hms.ipd.application.dto.AdmissionResponseDTO;
import com.res.hms.ipd.application.mapper.IpdMapper;
import com.res.hms.ipd.domain.Admission;
import com.res.hms.ipd.domain.AdmissionStatus;
import com.res.hms.ipd.domain.Bed;
import com.res.hms.ipd.domain.BedStatus;
import com.res.hms.ipd.infrastructure.repository.AdmissionRepository;
import com.res.hms.ipd.infrastructure.repository.BedRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdmissionService {

  private final AdmissionRepository admissionRepository;
  private final BedRepository bedRepository;
  private final PatientRepository patientRepository;
  private final UserAccountRepository userRepository;
  private final IpdMapper ipdMapper;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;
  private final JdbcTemplate jdbcTemplate;

  @Transactional
  public AdmissionResponseDTO admit(AdmissionRequestDTO dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();

    // Validate Patient
    Patient patient =
        resourceLookup.findRequired(
            dto.getPatientId(), hospitalId, patientRepository::findByIdAndHospitalId, "Patient");

    // Validate Bed
    Bed bed =
        resourceLookup.findRequired(
            dto.getBedId(), hospitalId, bedRepository::findByIdAndHospitalId, "Bed");
    if (bed.getStatus() != BedStatus.AVAILABLE) {
      throw new ApiException("BED_NOT_AVAILABLE", "Bed is not available", HttpStatus.BAD_REQUEST);
    }

    // Validate Doctor
    UserAccount doctor =
        resourceLookup.findRequired(
            dto.getDoctorId(), hospitalId, userRepository::findByIdAndHospitalId, "Doctor");

    // Generate Admission Number
    String admissionNumber = generateAdmissionNumber();

    // Create Admission
    Admission admission = new Admission();
    admission.setHospitalId(hospitalId);
    admission.setAdmissionNumber(admissionNumber);
    admission.setPatientId(patient.getId());
    admission.setDoctorId(doctor.getId());
    admission.setBedId(bed.getId());
    admission.setAdmissionReason(dto.getAdmissionReason());
    admission.setRemarks(dto.getRemarks());
    admission.setStatus(AdmissionStatus.ADMITTED);

    // Update Bed Status
    bed.setStatus(BedStatus.OCCUPIED);
    bedRepository.save(bed);

    return ipdMapper.toDto(admissionRepository.save(admission));
  }

  @Transactional(readOnly = true)
  public AdmissionResponseDTO get(UUID id) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Admission admission =
        resourceLookup.findRequired(
            id, hospitalId, admissionRepository::findByIdAndHospitalId, "Admission");
    return ipdMapper.toDto(admission);
  }

  @Transactional(readOnly = true)
  public PageResponse<AdmissionResponseDTO> list(Pageable pageable) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Page<Admission> admissions =
        admissionRepository.findAllByHospitalIdOrderByCreatedAtDesc(hospitalId, pageable);
    return PageResponse.from(admissions.map(ipdMapper::toDto));
  }

  @Transactional
  public AdmissionResponseDTO updateStatus(UUID id, AdmissionStatus status) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Admission admission =
        resourceLookup.findRequired(
            id, hospitalId, admissionRepository::findByIdAndHospitalId, "Admission");

    AdmissionStatus oldStatus = admission.getStatus();
    admission.setStatus(status);

    if ((status == AdmissionStatus.DISCHARGED || status == AdmissionStatus.CANCELLED)
        && oldStatus == AdmissionStatus.ADMITTED) {
      Bed bed = admission.getBed();
      if (bed == null) {
        bed =
            bedRepository
                .findByIdAndHospitalId(admission.getBedId(), hospitalId)
                .orElseThrow(
                    () -> new ApiException("BED_NOT_FOUND", "Bed not found", HttpStatus.NOT_FOUND));
      }
      bed.setStatus(BedStatus.AVAILABLE);
      bedRepository.save(bed);
    }

    return ipdMapper.toDto(admissionRepository.save(admission));
  }

  private String generateAdmissionNumber() {
    String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    Long seq =
        jdbcTemplate.queryForObject("SELECT nextval('ipd.admission_number_seq')", Long.class);
    return String.format("ADM-%s-%04d", datePart, seq);
  }
}
