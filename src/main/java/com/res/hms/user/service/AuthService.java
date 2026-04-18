package com.res.hms.user.service;

import com.res.hms.common.exception.ApiException;
import com.res.hms.domain.Hospital;
import com.res.hms.domain.RoleCode;
import com.res.hms.repository.HospitalRepository;
import com.res.hms.user.dto.HospitalMembershipResponse;
import com.res.hms.user.dto.LoginRequest;
import com.res.hms.user.dto.UserProfileResponse;
import com.res.hms.user.security.HospitalUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final HospitalRepository hospitalRepository;

    public void login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password())
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);
        } catch (BadCredentialsException exception) {
            throw new ApiException("AUTH_ERROR", "Invalid username or password", HttpStatus.UNAUTHORIZED);
        }
    }

    public UserProfileResponse selectHospital(Long hospitalId, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof HospitalUserPrincipal principal)) {
            throw new ApiException("AUTH_ERROR", "Authentication is required", HttpStatus.UNAUTHORIZED);
        }

        Set<RoleCode> selectedRoles = new HashSet<>();
        // Add global roles (e.g. SUPER_ADMIN)
        selectedRoles.addAll(principal.hospitalRoles().getOrDefault(null, Set.of()));

        if (hospitalId != null) {
            Set<RoleCode> hospitalScopedRoles = principal.hospitalRoles().get(hospitalId);
            if (hospitalScopedRoles == null) {
                throw new ApiException("AUTH_ERROR", "User has no access to this hospital", HttpStatus.FORBIDDEN);
            }
            selectedRoles.addAll(hospitalScopedRoles);
        }

        // Create a new principal with the selected context
        HospitalUserPrincipal refinedPrincipal = new HospitalUserPrincipal(
                principal.userId(),
                principal.username(),
                principal.password(),
                principal.active(),
                principal.hospitalRoles(),
                hospitalId,
                selectedRoles
        );

        Authentication refinedAuth = new UsernamePasswordAuthenticationToken(
                refinedPrincipal,
                null,
                refinedPrincipal.getAuthorities()
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(refinedAuth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        return toProfileResponse(refinedPrincipal);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());
    }

    public UserProfileResponse currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof HospitalUserPrincipal userPrincipal)) {
            throw new ApiException("AUTH_ERROR", "Authentication is required", HttpStatus.UNAUTHORIZED);
        }

        return toProfileResponse(userPrincipal);
    }

    private UserProfileResponse toProfileResponse(HospitalUserPrincipal principal) {
        List<HospitalMembershipResponse> memberships = new ArrayList<>();
        if (principal.hospitalRoles() != null) {
            for (Map.Entry<Long, Set<RoleCode>> entry : principal.hospitalRoles().entrySet()) {
                if (entry.getKey() != null) {
                    Hospital hospital = hospitalRepository.findById(entry.getKey()).orElseThrow();
                    memberships.add(new HospitalMembershipResponse(
                            hospital.getId(),
                            hospital.getName(),
                            hospital.getCode(),
                            entry.getValue()
                    ));
                }
            }
        }

        return new UserProfileResponse(
                principal.userId(),
                principal.hospitalId(),
                principal.username(),
                principal.roleCodes(),
                memberships
        );
    }
}
