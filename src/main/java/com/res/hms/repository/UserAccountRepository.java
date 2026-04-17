package com.res.hms.repository;

import com.res.hms.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    boolean existsByUsername(String username);
    List<UserAccount> findAllByHospitalIdOrderByCreatedAtDesc(Long hospitalId);
    Optional<UserAccount> findByIdAndHospitalId(Long id, Long hospitalId);
}
