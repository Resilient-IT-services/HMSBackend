package com.res.hms.repository;

import com.res.hms.domain.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
  Optional<UserAccount> findByUsername(String username);

  boolean existsByUsername(String username);

  @Query(
      "SELECT DISTINCT u FROM UserAccount u JOIN u.assignments a WHERE a.hospitalId = :hospitalId ORDER BY u.createdAt DESC")
  List<UserAccount> findAllByHospitalId(@Param("hospitalId") Long hospitalId);

  @Query(
      "SELECT u FROM UserAccount u JOIN u.assignments a WHERE u.id = :id AND a.hospitalId = :hospitalId")
  Optional<UserAccount> findByIdAndHospitalId(
      @Param("id") Long id, @Param("hospitalId") Long hospitalId);
}
