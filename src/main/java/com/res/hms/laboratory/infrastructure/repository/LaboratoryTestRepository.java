package com.res.hms.laboratory.infrastructure.repository;

import com.res.hms.laboratory.domain.LaboratoryTest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LaboratoryTestRepository extends JpaRepository<LaboratoryTest, UUID> {

  @Query("SELECT COUNT(l) > 0 FROM LaboratoryTest l WHERE LOWER(l.testCode) = LOWER(:testCode)")
  boolean existsByTestCodeIgnoreCase(@Param("testCode") String testCode);

  @Query("SELECT COUNT(l) > 0 FROM LaboratoryTest l WHERE LOWER(l.testName) = LOWER(:testName)")
  boolean existsByTestNameIgnoreCase(@Param("testName") String testName);

  List<LaboratoryTest> findByActiveOrderByTestNameAsc(boolean active);
}
