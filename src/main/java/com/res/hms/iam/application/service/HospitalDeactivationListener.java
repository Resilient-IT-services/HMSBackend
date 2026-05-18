package com.res.hms.iam.application.service;

import com.res.hms.hospital.domain.HospitalStatus;
import com.res.hms.hospital.domain.event.HospitalStatusChangedEvent;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalDeactivationListener {

  private final UserAccountRepository userAccountRepository;

  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  public void onHospitalStatusChanged(HospitalStatusChangedEvent event) {
    if (event.getNewStatus() == HospitalStatus.INACTIVE 
        || event.getNewStatus() == HospitalStatus.SUSPENDED) {
      log.info("Hospital {} deactivated/suspended. Processing user deactivations.", event.getHospitalId());
      
      List<UserAccount> users = userAccountRepository.findAllByHospitalId(event.getHospitalId());
      
      for (UserAccount user : users) {
        if (user.isSuperuser()) {
            continue; // Don't deactivate superusers
        }

        // Deactivate the assignment for this hospital
        user.getContracts().stream()
            .filter(a -> a.getHospitalId().equals(event.getHospitalId()))
            .forEach(a -> a.setActive(false));

        // Check if user has any OTHER active assignments for ACTIVE or ONBOARDING hospitals
        boolean hasOtherActiveHospital = user.getContracts().stream()
            .anyMatch(a -> !a.getHospitalId().equals(event.getHospitalId()) && 
                           a.isActive() && 
                           a.getHospital() != null && 
                           (a.getHospital().getStatus() == HospitalStatus.ACTIVE ||
                            a.getHospital().getStatus() == HospitalStatus.ONBOARDING));
        
        if (!hasOtherActiveHospital) {
          log.info("Deactivating user {} as they have no other active hospital memberships.", user.getUsername());
          user.setActive(false);
        }
      }
      
      userAccountRepository.saveAll(users);
    }
  }
}
