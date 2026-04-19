package com.res.hms.user.service;

import com.res.hms.domain.UserAccount;
import com.res.hms.user.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public UserResponse toResponse(UserAccount userAccount) {
    return new UserResponse(
        userAccount.getId(),
        userAccount.getUsername(),
        userAccount.getRoleCodes(),
        userAccount.isActive(),
        userAccount.getCreatedAt());
  }
}
