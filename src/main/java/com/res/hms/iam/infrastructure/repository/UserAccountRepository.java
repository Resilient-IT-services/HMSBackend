package com.res.hms.iam.infrastructure.repository;

import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository
    extends JpaRepository<UserAccount, UUID>, JpaSpecificationExecutor<UserAccount> {
  @Query(
      "SELECT DISTINCT u FROM UserAccount u "
          + "LEFT JOIN FETCH u.assignments a "
          + "LEFT JOIN FETCH a.role r "
          + "LEFT JOIN FETCH u.doctorProfile d "
          + "WHERE u.username = :username AND u.deleted = false")
  Optional<UserAccount> findByUsername(@Param("username") String username);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  boolean existsByEmailAndIdNot(String email, UUID id);

  List<UserAccount> findAllByDeletedFalse();

  @Query(
      "SELECT DISTINCT u FROM UserAccount u "
          + "LEFT JOIN FETCH u.assignments a "
          + "LEFT JOIN FETCH a.role r "
          + "LEFT JOIN FETCH u.doctorProfile d "
          + "WHERE (:hospitalId IS NULL OR a.hospitalId = :hospitalId) "
          + "AND (:active IS NULL OR u.active = :active) "
          + "AND (:deleted IS NULL OR u.deleted = :deleted) "
          + "AND (:roleCode IS NULL OR r.code = :roleCode) "
          + "AND (:search IS NULL OR "
          + "     LOWER(u.fullName) LIKE :search OR "
          + "     LOWER(u.email) LIKE :search OR "
          + "     LOWER(u.username) LIKE :search)")
  Page<UserAccount> searchUsers(
      @Param("hospitalId") UUID hospitalId,
      @Param("active") Boolean active,
      @Param("deleted") Boolean deleted,
      @Param("roleCode") RoleCode roleCode,
      @Param("search") String search,
      Pageable pageable);

  @Query(
      "SELECT DISTINCT u FROM UserAccount u JOIN u.assignments a WHERE a.hospitalId = :hospitalId AND u.deleted = false ORDER BY u.createdAt DESC")
  List<UserAccount> findAllByHospitalId(@Param("hospitalId") UUID hospitalId);

  @Query(
      "SELECT u FROM UserAccount u JOIN u.assignments a WHERE u.id = :id AND a.hospitalId = :hospitalId AND u.deleted = false")
  Optional<UserAccount> findByIdAndHospitalId(
      @Param("id") UUID id, @Param("hospitalId") UUID hospitalId);
}
