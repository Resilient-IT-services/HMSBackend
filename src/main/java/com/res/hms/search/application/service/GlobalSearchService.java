package com.res.hms.search.application.service;

import com.res.hms.clinical.application.patient.repository.PatientRepository;
import com.res.hms.core.api.PageResponse;
import com.res.hms.hospital.domain.Hospital;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import com.res.hms.search.application.dto.SearchResultDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GlobalSearchService {

  private final HospitalRepository hospitalRepository;
  private final UserAccountRepository userRepository;
  private final PatientRepository patientRepository;

  public PageResponse<SearchResultDTO> search(
      String query, UUID currentHospitalId, boolean isSuperAdmin, int page, int size) {
    List<SearchResultDTO> results = new ArrayList<>();

    if (query == null || query.trim().length() < 2) {
      return PageResponse.from(new PageImpl<>(results, PageRequest.of(page, size), 0));
    }

    String lowerQuery = query.toLowerCase();

    // 1. Search Hospitals (Top 5)
    if (isSuperAdmin) {
      List<Hospital> hospitals =
          hospitalRepository
              .findAll(
                  (Specification<Hospital>)
                      (root, q, cb) ->
                          cb.or(
                              cb.like(cb.lower(root.get("name")), "%" + lowerQuery + "%"),
                              cb.like(cb.lower(root.get("code")), "%" + lowerQuery + "%")),
                  PageRequest.of(0, 5))
              .getContent();
      results.addAll(
          hospitals.stream()
              .map(
                  h ->
                      SearchResultDTO.builder()
                          .id(h.getId())
                          .type("HOSPITAL")
                          .title(h.getName())
                          .subtitle("Code: " + h.getCode())
                          .link("/super_admin/hospitals/" + h.getId())
                          .build())
              .collect(Collectors.toList()));
    }

    // 2. Search Users (Top 5)
    String searchPattern = "%" + query.trim().toLowerCase() + "%";
    userRepository
        .searchUsers(
            isSuperAdmin ? null : currentHospitalId,
            null,
            false,
            null,
            searchPattern,
            PageRequest.of(0, 5))
        .getContent()
        .forEach(
            u -> {
              results.add(
                  SearchResultDTO.builder()
                      .id(u.getId())
                      .type("USER")
                      .title(u.getFullName() != null ? u.getFullName() : u.getUsername())
                      .subtitle(u.getEmail())
                      .link("/admin/users/" + u.getId())
                      .build());
            });

    // 3. Search Patients (Top 5)
    if (currentHospitalId != null) {
      patientRepository
          .search(currentHospitalId, searchPattern, PageRequest.of(0, 5))
          .getContent()
          .forEach(
              p -> {
                results.add(
                    SearchResultDTO.builder()
                        .id(p.getId())
                        .type("PATIENT")
                        .title(p.getFullName())
                        .subtitle(p.getPhone())
                        .link("/reception/patients/" + p.getId())
                        .build());
              });
    }

    Page<SearchResultDTO> resultPage =
        new PageImpl<>(results, PageRequest.of(page, size), results.size());
    return PageResponse.from(resultPage);
  }
}
