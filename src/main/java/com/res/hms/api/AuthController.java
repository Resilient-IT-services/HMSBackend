package com.res.hms.api;

import com.res.hms.common.api.ApiResponse;
import com.res.hms.user.dto.LoginRequest;
import com.res.hms.user.dto.SelectHospitalRequest;
import com.res.hms.user.dto.UserProfileResponse;
import com.res.hms.user.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  public ApiResponse<Void> login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    authService.login(request, httpRequest, httpResponse);
    return ApiResponse.success(null);
  }

  @PostMapping("/select-hospital")
  public ApiResponse<UserProfileResponse> selectHospital(
      @Valid @RequestBody SelectHospitalRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    return ApiResponse.success(
        authService.selectHospital(request.hospitalId(), httpRequest, httpResponse));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
    authService.logout(request, response);
    return ApiResponse.success(null);
  }

  @GetMapping("/me")
  public ApiResponse<UserProfileResponse> currentUser() {
    return ApiResponse.success(authService.currentUser());
  }
}
