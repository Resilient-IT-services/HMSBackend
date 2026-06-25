package com.res.hms.ipd.application.service;

import com.res.hms.clinical.domain.Patient;
import com.res.hms.clinical.infrastructure.repository.PatientRepository;
import com.res.hms.core.api.PageResponse;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.core.util.ResourceLookup;
import com.res.hms.iam.application.service.AuthService;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import com.res.hms.ipd.application.dto.AdmissionRequestDTO;
import com.res.hms.ipd.application.dto.AdmissionResponseDTO;
import com.res.hms.ipd.application.dto.BedTransferResponse;
import com.res.hms.ipd.application.dto.TransferBedRequest;
import com.res.hms.ipd.application.mapper.IpdMapper;
import com.res.hms.ipd.domain.Admission;
import com.res.hms.ipd.domain.AdmissionStatus;
import com.res.hms.ipd.domain.Bed;
import com.res.hms.ipd.domain.BedStatus;
import com.res.hms.ipd.domain.BedTransferHistory;
import com.res.hms.ipd.infrastructure.repository.AdmissionRepository;
import com.res.hms.ipd.infrastructure.repository.BedRepository;
import com.res.hms.ipd.infrastructure.repository.BedTransferHistoryRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
  private final BedTransferHistoryRepository bedTransferHistoryRepository;
  private final AuthService authService;

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

  @Transactional
  public BedTransferResponse transferBed(UUID admissionId, TransferBedRequest dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    UUID transferredBy = authService.getAuthenticatedPrincipal().userId();

    // 1. Fetch and Validate Admission
    Admission admission =
        resourceLookup.findRequired(
            admissionId, hospitalId, admissionRepository::findByIdAndHospitalId, "Admission");

    if (admission.getStatus() != AdmissionStatus.ADMITTED) {
      throw new ApiException(
          "INVALID_ADMISSION_STATUS",
          "Admission must be ADMITTED to perform bed transfer",
          HttpStatus.BAD_REQUEST);
    }

    // 2. Fetch and Validate Target Bed
    Bed newBed =
        resourceLookup.findRequired(
            dto.getToBedId(), hospitalId, bedRepository::findByIdAndHospitalId, "Bed");

    // 3. Target bed cannot equal current bed
    if (admission.getBedId().equals(newBed.getId())) {
      throw new ApiException(
          "INVALID_BED_TRANSFER",
          "Target bed cannot be the same as the current bed",
          HttpStatus.BAD_REQUEST);
    }

    if (newBed.getStatus() != BedStatus.AVAILABLE) {
      throw new ApiException(
          "BED_NOT_AVAILABLE", "Target bed is not available", HttpStatus.BAD_REQUEST);
    }

    // 4. Release Old Bed
    Bed oldBed = admission.getBed();
    if (oldBed == null) {
      oldBed =
          bedRepository
              .findByIdAndHospitalId(admission.getBedId(), hospitalId)
              .orElseThrow(
                  () ->
                      new ApiException(
                          "BED_NOT_FOUND", "Current bed not found", HttpStatus.NOT_FOUND));
    }
    oldBed.setStatus(BedStatus.AVAILABLE);
    bedRepository.save(oldBed);

    // 5. Occupy New Bed
    newBed.setStatus(BedStatus.OCCUPIED);
    bedRepository.save(newBed);

    // 6. Update Admission bed assignment
    admission.setBedId(newBed.getId());
    admission.setBed(newBed);
    admissionRepository.save(admission);

    // 7. Create Transfer History record
    BedTransferHistory history = new BedTransferHistory();
    history.setHospitalId(hospitalId);
    history.setAdmissionId(admission.getId());
    history.setAdmission(admission);
    history.setFromBedId(oldBed.getId());
    history.setFromBed(oldBed);
    history.setToBedId(newBed.getId());
    history.setToBed(newBed);
    history.setTransferReason(dto.getTransferReason());
    history.setRemarks(dto.getRemarks());
    history.setTransferredBy(transferredBy);
    history.setTransferredByUser(userRepository.findById(transferredBy).orElse(null));
    history.setTransferredAt(Instant.now());

    bedTransferHistoryRepository.save(history);

    return ipdMapper.toDto(history);
  }

  @Transactional(readOnly = true)
  public List<BedTransferResponse> getTransfers(UUID admissionId) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    // Validate Admission exists
    resourceLookup.findRequired(
        admissionId, hospitalId, admissionRepository::findByIdAndHospitalId, "Admission");

    List<BedTransferHistory> historyList =
        bedTransferHistoryRepository.findAllByAdmissionIdAndHospitalIdOrderByTransferredAtDesc(
            admissionId, hospitalId);

    return historyList.stream().map(ipdMapper::toDto).toList();
  }

  private String generateAdmissionNumber() {
    String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    Long seq =
        jdbcTemplate.queryForObject("SELECT nextval('ipd.admission_number_seq')", Long.class);
    return String.format("ADM-%s-%04d", datePart, seq);
  }

  @Transactional(readOnly = true)
  public List<AdmissionResponseDTO> getMyPatients() {
    UUID hospitalId = hospitalContext.requireHospitalId();
    UUID doctorId = authService.getAuthenticatedPrincipal().userId();

    List<Admission> admissions =
        admissionRepository.findAllByDoctorIdAndStatusAndHospitalIdOrderByAdmissionDateTimeDesc(
            doctorId, AdmissionStatus.ADMITTED, hospitalId);

    return admissions.stream().map(ipdMapper::toDto).toList();
  }
}
