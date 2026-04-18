package com.res.hms;

import com.res.hms.domain.Hospital;
import com.res.hms.domain.Role;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.UserAccount;
import com.res.hms.domain.UserRoleAssignment;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.repository.RoleRepository;
import com.res.hms.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestDataHelper {

    @Autowired
    private HospitalRepository hospitalRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long createHospitalIfMissing(String code, String name) {
        return hospitalRepository.findByCode(code)
                .map(Hospital::getId)
                .orElseGet(() -> {
                    Hospital h = new Hospital();
                    h.setCode(code);
                    h.setName(name);
                    h.setAddress("Address");
                    h.setStatus("ACTIVE");
                    return hospitalRepository.save(h).getId();
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createUserIfMissing(String username, RoleCode roleCode, Long hospitalId, String password, PasswordEncoder encoder, UserAccountRepository userRepo, RoleRepository roleRepo) {
        if (userRepo.existsByUsername(username)) return;

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(password));
        user.setFullName(username + " Full Name");
        user.setActive(true);

        Role role = roleRepo.findByCode(roleCode)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleCode));

        UserRoleAssignment assignment = new UserRoleAssignment();
        assignment.setUser(user);
        assignment.setHospitalId(hospitalId);
        assignment.setRole(role);
        user.getAssignments().add(assignment);

        userRepo.save(user);
    }
}
