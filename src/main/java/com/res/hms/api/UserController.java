package com.res.hms.api;

import com.res.hms.common.api.ApiResponse;
import com.res.hms.user.dto.UserFullProfileDTO;
import com.res.hms.user.dto.UserRequestDTO;
import com.res.hms.user.dto.UserSummaryDTO;
import com.res.hms.user.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
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

  @GetMapping
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
  public ApiResponse<List<UserSummaryDTO>> list(
      @RequestParam(defaultValue = "false") boolean includeDeleted) {
    return ApiResponse.success(userService.listUsers(includeDeleted));
  }

  @GetMapping("/{id}/full-profile")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
  public ApiResponse<UserFullProfileDTO> getFullProfile(@PathVariable UUID id) {
    return ApiResponse.success(userService.getFullProfile(id));
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
  public ApiResponse<UserFullProfileDTO> create(@Valid @RequestBody UserRequestDTO request) {
    return ApiResponse.success(userService.createUser(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
  public ApiResponse<UserFullProfileDTO> update(
      @PathVariable UUID id, @Valid @RequestBody UserRequestDTO request) {
    return ApiResponse.success(userService.updateUser(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    userService.deleteUser(id);
    return ApiResponse.success(null);
  }
}
