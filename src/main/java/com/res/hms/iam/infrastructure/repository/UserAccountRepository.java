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
          + "LEFT JOIN FETCH u.contracts a "
          + "LEFT JOIN FETCH a.role r "
          + "LEFT JOIN FETCH a.hospital h "
          + "LEFT JOIN FETCH u.doctorProfile d "
          + "WHERE LOWER(TRIM(u.username)) = LOWER(TRIM(:username)) AND u.deleted = false")
  Optional<UserAccount> findByUsername(@Param("username") String username);

  @Query(
      "SELECT COUNT(u) > 0 FROM UserAccount u WHERE LOWER(TRIM(u.username)) = LOWER(TRIM(:username))")
  boolean existsByUsername(@Param("username") String username);

  @Query("SELECT COUNT(u) > 0 FROM UserAccount u WHERE LOWER(TRIM(u.email)) = LOWER(TRIM(:email))")
  boolean existsByEmail(@Param("email") String email);

  @Query(
      "SELECT COUNT(u) > 0 FROM UserAccount u WHERE LOWER(TRIM(u.email)) = LOWER(TRIM(:email)) AND u.id <> :id")
  boolean existsByEmailAndIdNot(@Param("email") String email, @Param("id") UUID id);

  List<UserAccount> findAllByDeletedFalse();

  @Query(
      "SELECT DISTINCT u FROM UserAccount u "
          + "LEFT JOIN FETCH u.contracts a "
          + "LEFT JOIN FETCH a.role r "
          + "LEFT JOIN FETCH a.hospital h "
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
      "SELECT DISTINCT u FROM UserAccount u "
          + "LEFT JOIN FETCH u.contracts a "
          + "LEFT JOIN FETCH a.hospital h "
          + "WHERE EXISTS (SELECT 1 FROM UserContract ura WHERE ura.user = u AND ura.hospitalId = :hospitalId) "
          + "AND u.deleted = false "
          + "ORDER BY u.createdAt DESC")
  List<UserAccount> findAllByHospitalId(@Param("hospitalId") UUID hospitalId);

  @Query(
      "SELECT u FROM UserAccount u JOIN u.contracts a WHERE u.id = :id AND a.hospitalId = :hospitalId AND u.deleted = false")
  Optional<UserAccount> findByIdAndHospitalId(
      @Param("id") UUID id, @Param("hospitalId") UUID hospitalId);

  long countByActive(boolean active);

  @Query("SELECT u FROM UserAccount u WHERE u.deleted = false ORDER BY u.createdAt DESC")
  List<UserAccount> findRecentUsers(Pageable pageable);

  @Query("SELECT COUNT(u) FROM UserAccount u WHERE u.lastLoginAt >= :date")
  long countActiveUsersSince(@Param("date") java.time.Instant date);
}
