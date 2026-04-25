package com.res.hms.doctor.service;

import com.res.hms.common.api.PageResponse;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.doctor.dto.DoctorResponse;
import com.res.hms.doctor.repository.DoctorRepository;
import com.res.hms.domain.Doctor;
import com.res.hms.domain.DoctorHospitalDetails;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.repository.DoctorHospitalDetailsRepository;
import com.res.hms.user.dto.DoctorHospitalDetailsDTO;
import com.res.hms.user.dto.UserDetailsDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DoctorService {

  private final DoctorRepository doctorRepository;
  private final DoctorHospitalDetailsRepository doctorHospitalDetailsRepository;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;

  @Transactional
  public void updateProfile(UserAccount user, UserDetailsDTO.DoctorInfo info) {
    if (info == null) {
      return;
    }
    Doctor doctor =
        doctorRepository
            .findByUserId(user.getId())
            .orElseGet(
                () -> {
                  Doctor d = new Doctor();
                  d.setUser(user);
                  return d;
                });
    if (info.getSpecialization() != null) {
      doctor.setSpecialization(info.getSpecialization());
    }
    if (info.getBio() != null) {
      doctor.setBio(info.getBio());
    }
    if (info.getLicenseNumber() != null) {
      doctor.setLicenseNumber(info.getLicenseNumber());
    }
    doctorRepository.save(doctor);
    user.setDoctor(doctor);
  }

  @Transactional
  public void updateHospitalDetails(UserRoleAssignment assignment, DoctorHospitalDetailsDTO d) {
    if (d == null) {
      return;
    }
    DoctorHospitalDetails det =
        doctorHospitalDetailsRepository
            .findById(assignment.getId())
            .orElseGet(
                () -> {
                  DoctorHospitalDetails newDet = new DoctorHospitalDetails();
                  newDet.setAssignment(assignment);
                  return newDet;
                });
    if (d.getConsultationFee() != null) {
      det.setConsultationFee(d.getConsultationFee());
    }
    if (d.getFollowUpDays() != null) {
      det.setFollowUpDays(d.getFollowUpDays());
    }
    if (d.getCommissionType() != null) {
      det.setCommissionType(d.getCommissionType());
    }
    if (d.getCommissionValue() != null) {
      det.setCommissionValue(d.getCommissionValue());
    }
    det.setVisitingConsultant(d.isVisitingConsultant());
    det.setUpdatedAt(LocalDateTime.now());
    doctorHospitalDetailsRepository.save(det);
    assignment.setDoctorDetails(det);
  }

  public PageResponse<DoctorResponse> list(int page, int size) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return PageResponse.from(
        doctorRepository
            .findAllByHospitalIdOrderByCreatedAtDesc(hospitalId, PageRequest.of(page, size))
            .map(this::toResponse));
  }

  public Doctor requireDoctor(UUID id, UUID hospitalId) {
    return resourceLookup.findRequired(
        id, hospitalId, doctorRepository::findByIdAndHospitalId, "Doctor");
  }

  private DoctorResponse toResponse(Doctor doctor) {
    UserAccount user = doctor.getUser();
    UUID activeHospitalId = hospitalContext.getHospitalIdOrNull();

    BigDecimal consultationFee = BigDecimal.ZERO;
    Integer followUpDays = 0;

    if (activeHospitalId != null && user != null) {
      Optional<DoctorHospitalDetails> details =
          doctorHospitalDetailsRepository.findByUserIdAndHospitalId(user.getId(), activeHospitalId);
      if (details.isPresent()) {
        consultationFee = details.get().getConsultationFee();
        followUpDays = details.get().getFollowUpDays();
      }
    }

    return new DoctorResponse(
        doctor.getId(),
        user != null ? user.getFullName() : "N/A",
        user != null ? user.getGender() : null,
        user != null ? user.getDob() : null,
        user != null ? user.getPhone() : null,
        doctor.getSpecialization(),
        consultationFee,
        followUpDays,
        doctor.getEducation(),
        doctor.getCreatedAt());
  }
}
