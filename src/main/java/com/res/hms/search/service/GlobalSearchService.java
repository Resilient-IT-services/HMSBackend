package com.res.hms.search.service;

import com.res.hms.domain.Hospital;
import com.res.hms.domain.Patient;
import com.res.hms.domain.UserAccount;
import com.res.hms.patient.repository.PatientRepository;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.repository.UserAccountRepository;
import com.res.hms.search.dto.SearchResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GlobalSearchService {

    private final HospitalRepository hospitalRepository;
    private final UserAccountRepository userRepository;
    private final PatientRepository patientRepository;

    public List<SearchResultDTO> search(String query, UUID currentHospitalId, boolean isSuperAdmin) {
        List<SearchResultDTO> results = new ArrayList<>();

        if (query == null || query.trim().length() < 2) {
            return results;
        }

        String lowerQuery = query.toLowerCase();

        // 1. Search Hospitals (Only for Super Admin or if no hospital restricted)
        if (isSuperAdmin) {
            List<Hospital> hospitals = hospitalRepository.findAll((Specification<Hospital>) (root, q, cb) -> 
                cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + lowerQuery + "%"),
                    cb.like(cb.lower(root.get("code")), "%" + lowerQuery + "%")
                )
            );
            results.addAll(hospitals.stream()
                .limit(5)
                .map(h -> SearchResultDTO.builder()
                    .id(h.getId())
                    .type("HOSPITAL")
                    .title(h.getName())
                    .subtitle("Code: " + h.getCode())
                    .link("/super_admin/hospitals/" + h.getId())
                    .build())
                .collect(Collectors.toList()));
        }

        // 2. Search Users
        // We use the existing searchUsers method but adapted for global search
        userRepository.searchUsers(isSuperAdmin ? null : currentHospitalId, null, false, null, query, PageRequest.of(0, 5))
            .getContent().forEach(u -> {
                results.add(SearchResultDTO.builder()
                    .id(u.getId())
                    .type("USER")
                    .title(u.getFullName() != null ? u.getFullName() : u.getUsername())
                    .subtitle(u.getEmail())
                    .link("/admin/users/" + u.getId())
                    .build());
            });

        // 3. Search Patients
        if (currentHospitalId != null) {
            patientRepository.search(currentHospitalId, query, PageRequest.of(0, 5))
                .getContent().forEach(p -> {
                    results.add(SearchResultDTO.builder()
                        .id(p.getId())
                        .type("PATIENT")
                        .title(p.getFullName())
                        .subtitle(p.getPhone())
                        .link("/reception/patients/" + p.getId())
                        .build());
                });
        }

        return results;
    }
}
