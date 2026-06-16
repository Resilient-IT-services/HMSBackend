package com.res.hms.pharmacy.infrastructure.repository;

import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.pharmacy.domain.MedicineForm;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicineRepository
    extends JpaRepository<Medicine, UUID>, JpaSpecificationExecutor<Medicine> {

  Optional<Medicine> findByNameIgnoreCase(String name);

  @Query("SELECT m FROM Medicine m WHERE LOWER(m.name) = LOWER(:name)")
  java.util.List<Medicine> findByExactName(@Param("name") String name);

  Optional<Medicine> findByNameIgnoreCaseAndGenericNameIgnoreCaseAndStrengthIgnoreCaseAndForm(
      String name, String genericName, String strength, MedicineForm form);

  @Query(
      """
      SELECT COUNT(m) > 0 FROM Medicine m
      WHERE LOWER(m.name) = LOWER(:name)
        AND (:genericName IS NULL AND m.genericName IS NULL OR LOWER(m.genericName) = LOWER(:genericName))
        AND (:strength IS NULL AND m.strength IS NULL OR LOWER(m.strength) = LOWER(:strength))
        AND (:form IS NULL AND m.form IS NULL OR m.form = :form)
      """)
  boolean existsDuplicate(
      @Param("name") String name,
      @Param("genericName") String genericName,
      @Param("strength") String strength,
      @Param("form") MedicineForm form);

  long countByDeletedFalse();
}
