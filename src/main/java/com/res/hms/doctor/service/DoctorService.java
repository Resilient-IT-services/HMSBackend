package com.res.hms.doctor.service;

import com.res.hms.common.api.PageResponse;
import com.res.hms.common.audit.Auditable;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.doctor.dto.CreateDoctorRequest;
import com.res.hms.doctor.dto.DoctorResponse;
import com.res.hms.doctor.repository.DoctorRepository;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.UserAccount;
import com.res.hms.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DoctorService {

  private final DoctorRepository doctorRepository;
  private final UserAccountRepository userAccountRepository;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;

  @Transactional
  @Auditable(entity = "doctor", action = "CREATE")
  public DoctorResponse create(CreateDoctorRequest request) {
    Long hospitalId = hospitalContext.requireHospitalId();

    UserAccount user =
        resourceLookup.findRequired(
            request.userId(), hospitalId, userAccountRepository::findByIdAndHospitalId, "User");

    Doctor doctor =
        doctorRepository
            .findByUserId(user.getId())
            .orElseGet(
                () -> {
                  Doctor newDoc = new Doctor();
                  newDoc.setUser(user);
                  return newDoc;
                });
    doctor.setSpecialization(request.specialization());
    doctor.setConsultationFee(request.consultationFee());
    doctor.setFollowUpDays(request.followUpDays());
    doctorRepository.save(doctor);

    return toResponse(doctor);
  }

  public PageResponse<DoctorResponse> list(int page, int size, String search) {
    Long hospitalId = hospitalContext.requireHospitalId();
    PageRequest pageRequest = PageRequest.of(page, size);

    if (search != null && !search.trim().isEmpty()) {
      return PageResponse.from(
          doctorRepository.search(hospitalId, search.trim(), pageRequest).map(this::toResponse));
    }

    return PageResponse.from(
        doctorRepository
            .findAllByHospitalIdOrderByCreatedAtDesc(hospitalId, pageRequest)
            .map(this::toResponse));
  }

  public Doctor requireDoctor(Long id, Long hospitalId) {
    return resourceLookup.findRequired(
        id, hospitalId, doctorRepository::findByIdAndHospitalId, "Doctor");
  }

  private DoctorResponse toResponse(Doctor doctor) {
    UserAccount user = doctor.getUser();
    return new DoctorResponse(
        doctor.getId(),
        user != null ? user.getFullName() : "N/A",
        user != null ? user.getGender() : null,
        user != null ? user.getDob() : null,
        user != null ? user.getPhone() : null,
        doctor.getSpecialization(),
        doctor.getConsultationFee(),
        doctor.getFollowUpDays(),
        doctor.getCreatedAt());
  }
}
