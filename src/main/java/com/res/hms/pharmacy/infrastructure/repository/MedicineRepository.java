package com.res.hms.pharmacy.infrastructure.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.res.hms.pharmacy.domain.Medicine;

public interface MedicineRepository extends JpaRepository<Medicine, UUID> {

  Page<Medicine> findAllByOrderByCreatedAtDesc(Pageable pageable);

  @Query(
      """
      SELECT m FROM Medicine m
      WHERE lower(m.name) LIKE lower(concat('%', :search, '%'))
         OR lower(m.genericName) LIKE lower(concat('%', :search, '%'))
         OR lower(m.composition) LIKE lower(concat('%', :search, '%'))
      ORDER BY m.name
      """)
  Page<Medicine> searchMaster(@Param("search") String search, Pageable pageable);

  @Query(
      """
      SELECT DISTINCT m FROM Medicine m
      JOIN PharmacyStock s ON s.medicine = m
      WHERE s.hospitalId = :hospitalId
        AND s.quantity > 0
        AND (s.expiryDate IS NULL OR s.expiryDate >= CURRENT_DATE)
        AND (
          lower(m.name) LIKE lower(concat('%', :search, '%'))
          OR lower(m.genericName) LIKE lower(concat('%', :search, '%'))
          OR lower(m.composition) LIKE lower(concat('%', :search, '%'))
        )
      ORDER BY m.name
      """)
  Page<Medicine> searchAvailableForHospital(
      @Param("hospitalId") UUID hospitalId, @Param("search") String search, Pageable pageable);

  Optional<Medicine> findByNameIgnoreCase(String name);

  @Query("SELECT m FROM Medicine m WHERE LOWER(m.name) = LOWER(:name)")
  java.util.List<Medicine> findByExactName(@Param("name") String name);

  Optional<Medicine>
      findByNameIgnoreCaseAndGenericNameIgnoreCaseAndStrengthIgnoreCaseAndFormIgnoreCase(
          String name, String genericName, String strength, String form);

  @Query(
      """
      SELECT COUNT(m) > 0 FROM Medicine m
      WHERE LOWER(m.name) = LOWER(:name)
        AND (:genericName IS NULL AND m.genericName IS NULL OR LOWER(m.genericName) = LOWER(:genericName))
        AND (:strength IS NULL AND m.strength IS NULL OR LOWER(m.strength) = LOWER(:strength))
        AND (:form IS NULL AND m.form IS NULL OR LOWER(m.form) = LOWER(:form))
      """)
  boolean existsDuplicate(
      @Param("name") String name,
      @Param("genericName") String genericName,
      @Param("strength") String strength,
      @Param("form") String form);
}
