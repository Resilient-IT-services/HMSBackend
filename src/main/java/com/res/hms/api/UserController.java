package com.res.hms.api;

import com.res.hms.common.api.ApiResponse;
import com.res.hms.user.dto.AllocateRoleRequest;
import com.res.hms.user.dto.CreateUserRequest;
import com.res.hms.user.dto.UserResponse;
import com.res.hms.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @PostMapping
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
  public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
    return ApiResponse.success(userService.create(request));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
  public ApiResponse<List<UserResponse>> list(
      @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false")
          boolean includeDeleted) {
    return ApiResponse.success(userService.list(includeDeleted));
  }

  @PostMapping("/allocate")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
  public ApiResponse<UserResponse> allocate(@Valid @RequestBody AllocateRoleRequest request) {
    return ApiResponse.success(userService.allocateRoles(request));
  }

  @org.springframework.web.bind.annotation.PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
  public ApiResponse<UserResponse> update(
      @org.springframework.web.bind.annotation.PathVariable Long id,
      @Valid @RequestBody com.res.hms.user.dto.UpdateUserRequest request) {
    return ApiResponse.success(userService.update(id, request));
  }

  @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
  public ApiResponse<Void> delete(@org.springframework.web.bind.annotation.PathVariable Long id) {
    userService.delete(id);
    return ApiResponse.success(null);
  }
}
