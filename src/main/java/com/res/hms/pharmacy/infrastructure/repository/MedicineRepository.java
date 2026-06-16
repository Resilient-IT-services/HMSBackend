package com.res.hms.pharmacy.infrastructure.repository;

import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.pharmacy.domain.MedicineForm;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicineRepository
    extends JpaRepository<Medicine, UUID>, JpaSpecificationExecutor<Medicine> {

  Optional<Medicine> findByNameIgnoreCase(String name);

  @Query("SELECT m FROM Medicine m WHERE UPPER(m.name) = UPPER(:name) AND m.deleted = false")
  List<Medicine> findByExactName(@Param("name") String name);

  @Query(
      """
      SELECT COUNT(m) > 0 FROM Medicine m
      WHERE UPPER(m.name) = UPPER(:name)
      AND (:genericName IS NULL OR UPPER(m.genericName) = UPPER(:genericName))
      AND (:strength IS NULL OR UPPER(m.strength) = UPPER(:strength))
      AND (:form IS NULL OR m.form = :form)
      """)
  boolean existsDuplicate(
      @Param("name") String name,
      @Param("genericName") String genericName,
      @Param("strength") String strength,
      @Param("form") MedicineForm form);

  @Query(
      "SELECT UPPER(m.itemCode) FROM Medicine m WHERE m.itemCode IS NOT NULL AND m.deleted = false")
  Set<String> findAllItemCodes();

  @Query(
      "SELECT CONCAT(m.department, '_', UPPER(m.name)) FROM Medicine m WHERE m.department IS NOT NULL AND m.deleted = false")
  Set<String> findAllDeptItemNames();
}
