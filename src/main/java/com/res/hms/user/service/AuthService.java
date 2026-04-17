package com.res.hms.user.service;

import com.res.hms.common.exception.ApiException;
import com.res.hms.user.dto.LoginRequest;
import com.res.hms.user.dto.LoginResponse;
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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password())
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            HospitalUserPrincipal principal = (HospitalUserPrincipal) authentication.getPrincipal();
            return new LoginResponse(principal.userId(), principal.hospitalId(), principal.username(), principal.roleCode());
        } catch (BadCredentialsException exception) {
            throw new ApiException("AUTH_ERROR", "Invalid username or password", HttpStatus.UNAUTHORIZED);
        }
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());
    }

    public UserProfileResponse currentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            throw new ApiException("AUTH_ERROR", "Authentication is required", HttpStatus.UNAUTHORIZED);
        }

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof HospitalUserPrincipal userPrincipal)) {
            throw new ApiException("AUTH_ERROR", "Authentication is required", HttpStatus.UNAUTHORIZED);
        }

        return new UserProfileResponse(userPrincipal.userId(), userPrincipal.hospitalId(), userPrincipal.username(), userPrincipal.roleCode());
    }
}
