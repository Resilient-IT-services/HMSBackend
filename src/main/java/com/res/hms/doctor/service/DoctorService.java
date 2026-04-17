package com.res.hms.doctor.service;

import com.res.hms.audit.service.AuditLogService;
import com.res.hms.common.api.PageResponse;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.doctor.dto.CreateDoctorRequest;
import com.res.hms.doctor.dto.DoctorResponse;
import com.res.hms.doctor.repository.DoctorRepository;
import com.res.hms.domain.Doctor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final HospitalContext hospitalContext;
    private final AuditLogService auditLogService;

    @Transactional
    public DoctorResponse create(CreateDoctorRequest request) {
        Long hospitalId = hospitalContext.requireHospitalId();
        Doctor doctor = new Doctor();
        doctor.setHospitalId(hospitalId);
        doctor.setFullName(request.fullName());
        doctor.setGender(request.gender());
        doctor.setDob(request.dob());
        doctor.setPhone(request.phone());
        doctor.setSpecialization(request.specialization());
        doctorRepository.save(doctor);

        auditLogService.record(hospitalId, "doctor", doctor.getId(), "CREATE", "Created doctor " + doctor.getFullName());
        return toResponse(doctor);
    }

    public PageResponse<DoctorResponse> list(int page, int size) {
        Long hospitalId = hospitalContext.requireHospitalId();
        return PageResponse.from(doctorRepository.findAllByHospitalIdOrderByCreatedAtDesc(hospitalId, PageRequest.of(page, size))
                .map(this::toResponse));
    }

    public Doctor requireDoctor(Long id, Long hospitalId) {
        return doctorRepository.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Doctor not found", HttpStatus.NOT_FOUND));
    }

    private DoctorResponse toResponse(Doctor doctor) {
        return new DoctorResponse(
                doctor.getId(),
                doctor.getFullName(),
                doctor.getGender(),
                doctor.getDob(),
                doctor.getPhone(),
                doctor.getSpecialization(),
                doctor.getCreatedAt()
        );
    }
}
