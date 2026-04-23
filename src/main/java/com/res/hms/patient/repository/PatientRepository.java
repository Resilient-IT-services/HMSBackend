package com.res.hms.patient.repository;

import com.res.hms.common.repository.BaseRepository;
import com.res.hms.domain.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PatientRepository extends BaseRepository<Patient, Long> {


    // 🔥 REQUIRED for ID search
    Optional<Patient> findByIdAndHospitalId(Long id, Long hospitalId);

    // 🔥 MAIN SEARCH QUERY (name + phone + patientCode)
    @Query(
        "SELECT p FROM Patient p WHERE p.hospitalId = :hospitalId AND (" +
            "LOWER(p.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "p.phone LIKE CONCAT('%', :query, '%') OR " +
            "LOWER(p.patientCode) LIKE LOWER(CONCAT('%', :query, '%'))" +
            ") ORDER BY p.createdAt DESC"
    )
    Page<Patient> search(
        @Param("hospitalId") Long hospitalId,
        @Param("query") String query,
        Pageable pageable
    );


}
