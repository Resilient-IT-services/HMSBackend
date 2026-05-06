package com.res.hms.staff.repository;

import com.res.hms.domain.ReceptionistProfile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceptionistProfileRepository extends JpaRepository<ReceptionistProfile, UUID> {}
