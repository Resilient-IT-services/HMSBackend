package com.res.hms.patient.service;

import com.res.hms.common.api.PageResponse;
import com.res.hms.common.audit.Auditable;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.domain.Patient;
import com.res.hms.patient.dto.CreatePatientRequest;
import com.res.hms.patient.dto.PatientResponse;
import com.res.hms.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final HospitalContext hospitalContext;
    private final ResourceLookup resourceLookup;

    // ========================= CREATE =========================
    @Transactional
    @Auditable(entity = "patient", action = "CREATE")
    public PatientResponse create(CreatePatientRequest request) {
        Long hospitalId = hospitalContext.requireHospitalId();


        Patient patient = new Patient();
        patient.setHospitalId(hospitalId);
        patient.setFullName(request.fullName());
        patient.setGender(request.gender());
        patient.setDob(request.dob());
        patient.setPhone(request.phone());
        patient.setAddress(request.address());

// 🔥 Save first to generate ID
        Patient saved = patientRepository.save(patient);

// 🔥 Generate Patient Code (PAT-00001)
        String code = "PAT-" + String.format("%05d", saved.getId());
        saved.setPatientCode(code);

// 🔥 Save again with code
        patientRepository.save(saved);

        return toResponse(saved);


    }

    // ========================= LIST + SEARCH =========================
    public PageResponse<PatientResponse> list(int page, int size, String search) {
        Long hospitalId = hospitalContext.requireHospitalId();
        PageRequest pageRequest = PageRequest.of(page, size);


        if (search != null && !search.trim().isEmpty()) {

            String query = search.trim();

            // 🔥 CASE 1: numeric → search by ID
            if (query.matches("\\d+")) {
                Long id = Long.parseLong(query);

                Optional<Patient> patientOpt =
                    patientRepository.findByIdAndHospitalId(id, hospitalId);

                if (patientOpt.isPresent()) {
                    return PageResponse.single(toResponse(patientOpt.get()));
                }
            }

            // 🔥 CASE 2: text → search by name / phone / patientCode
            return PageResponse.from(
                patientRepository
                    .search(hospitalId, query, pageRequest)
                    .map(this::toResponse)
            );
        }

// 🔥 DEFAULT: all patients
        return PageResponse.from(
            patientRepository
                .findAllByHospitalIdOrderByCreatedAtDesc(hospitalId, pageRequest)
                .map(this::toResponse)
        );


    }

    // ========================= GET =========================
    public PatientResponse get(Long id) {
        Long hospitalId = hospitalContext.requireHospitalId();
        return toResponse(requirePatient(id, hospitalId));
    }

    // ========================= REQUIRE =========================
    public Patient requirePatient(Long id, Long hospitalId) {
        return resourceLookup.findRequired(
            id, hospitalId, patientRepository::findByIdAndHospitalId, "Patient");
    }

    // ========================= MAPPER =========================
    private PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
            patient.getId(),
            patient.getFullName(),
            patient.getPatientCode(),
            patient.getGender().name(),
            patient.getDob(),
            patient.getPhone(),
            patient.getAddress(),
            patient.getCreatedAt()
        );
    }
}
