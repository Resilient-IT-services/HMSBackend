package com.res.hms.user.api;

import com.res.hms.common.api.ApiResponse;
import com.res.hms.common.api.PageResponse;
import com.res.hms.user.dto.AdminResetPasswordRequest;
import com.res.hms.user.dto.UserDetailsDTO;
import com.res.hms.user.dto.UserSummaryDTO;
import com.res.hms.user.service.UserAggregateService;
import com.res.hms.user.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;
  private final UserAggregateService userAggregateService;

  @PostMapping("/{userId}/reset-password")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
  public ApiResponse<Void> resetPassword(
      @PathVariable UUID userId, @Valid @RequestBody AdminResetPasswordRequest request) {
    userService.resetPassword(userId, request);
    return ApiResponse.success(null);
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
  public ApiResponse<PageResponse<UserSummaryDTO>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String role,
      @RequestParam(required = false) Boolean active,
      @RequestParam(defaultValue = "false") boolean includeDeleted) {
    return ApiResponse.success(
        userService.listUsers(page, Math.min(size, 100), search, role, active, includeDeleted));
  }

  @GetMapping("/{userId}/full-profile")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
  public ApiResponse<UserDetailsDTO> getFullProfile(@PathVariable UUID userId) {
    return ApiResponse.success(userService.getFullProfile(userId));
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
  public ApiResponse<UserDetailsDTO> create(@Valid @RequestBody UserDetailsDTO request) {
    return ApiResponse.success(userAggregateService.saveUserAggregate(request));
  }

  @PutMapping("/{userId}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
  public ApiResponse<UserDetailsDTO> update(
      @PathVariable UUID userId, @Valid @RequestBody UserDetailsDTO request) {
    request.setId(userId);
    return ApiResponse.success(userAggregateService.saveUserAggregate(request));
  }

  @DeleteMapping("/{userId}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
  public ApiResponse<Void> delete(@PathVariable UUID userId) {
    userService.deleteUser(userId);
    return ApiResponse.success(null);
  }
}
