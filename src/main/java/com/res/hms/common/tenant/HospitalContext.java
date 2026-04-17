package com.res.hms.common.tenant;

import com.res.hms.common.exception.ApiException;
import com.res.hms.user.security.HospitalUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class HospitalContext {

    public Long requireHospitalId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new ApiException("AUTH_ERROR", "Authentication is required", HttpStatus.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof HospitalUserPrincipal hospitalUserPrincipal) {
            return hospitalUserPrincipal.hospitalId();
        }

        throw new ApiException("AUTH_ERROR", "Hospital context is missing", HttpStatus.UNAUTHORIZED);
    }

    public Long currentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof HospitalUserPrincipal hospitalUserPrincipal) {
            return hospitalUserPrincipal.userId();
        }

        return null;
    }
}
