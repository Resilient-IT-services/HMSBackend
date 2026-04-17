package com.res.hms.user.service;

import com.res.hms.domain.UserAccount;
import com.res.hms.repository.UserAccountRepository;
import com.res.hms.user.security.HospitalUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HospitalUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount userAccount = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!userAccount.isActive()) {
            throw new DisabledException("User account is inactive");
        }

        return new HospitalUserPrincipal(
                userAccount.getId(),
                userAccount.getHospitalId(),
                userAccount.getUsername(),
                userAccount.getPasswordHash(),
                userAccount.getRole().getCode(),
                userAccount.isActive()
        );
    }
}
