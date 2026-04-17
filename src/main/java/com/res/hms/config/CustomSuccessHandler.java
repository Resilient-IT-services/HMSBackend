package com.res.hms.config;

import com.res.hms.model.User;
import com.res.hms.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        String username = authentication.getName();

        User user = userRepository.findByUsername(username).get();

        user.setFailedAttempts(0);
        userRepository.save(user);

        String role = authentication.getAuthorities().iterator().next().getAuthority();

        switch (role) {
            case "ROLE_ADMIN":
                response.sendRedirect("/admin/dashboard");
                break;
            case "ROLE_DOCTOR":
                response.sendRedirect("/doctor/dashboard");
                break;
            case "ROLE_RECEPTIONIST":
                response.sendRedirect("/reception/dashboard");
                break;
            case "ROLE_PHARMACIST":
                response.sendRedirect("/pharmacy/dashboard");
                break;
            case "ROLE_LAB":
                response.sendRedirect("/lab/dashboard");
                break;
            default:
                response.sendRedirect("/dashboard");
        }
    }
}