package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserDeactivationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void deactivatedUserCannotLogin() throws Exception {
    // 1. Setup a deactivated user
    UserAccount inactiveUser = new UserAccount();
    inactiveUser.setUsername("inactive_user");
    inactiveUser.setPasswordHash(passwordEncoder.encode("password"));
    inactiveUser.setActive(false);
    userAccountRepository.saveAndFlush(inactiveUser);

    // 2. Try login - should fail
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"inactive_user\", \"password\": \"password\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void userCannotDeactivateThemselves() throws Exception {
      // 1. Setup an admin user
      UserAccount adminUser = new UserAccount();
      adminUser.setUsername("test_admin");
      adminUser.setPasswordHash(passwordEncoder.encode("password"));
      adminUser.setActive(true);
      adminUser.setSuperuser(true);
      userAccountRepository.saveAndFlush(adminUser);

      // 2. Login as this admin
      MockHttpSession session =
          (MockHttpSession)
              mockMvc
                  .perform(
                      post("/api/v1/auth/login")
                          .contentType(MediaType.APPLICATION_JSON)
                          .content("{\"username\": \"test_admin\", \"password\": \"password\"}"))
                  .andExpect(status().isOk())
                  .andReturn()
                  .getRequest()
                  .getSession(false);

      // 3. Try to deactivate themselves
      mockMvc
          .perform(
              put("/api/v1/users/" + adminUser.getId())
                  .session(session)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"username\": \"test_admin\", \"active\": false}"))
          .andExpect(status().isBadRequest()); // Expected to fail
  }

  @Test
  void userCannotDeleteThemselves() throws Exception {
      // 1. Setup an admin user
      UserAccount adminUser = new UserAccount();
      adminUser.setUsername("test_admin_delete");
      adminUser.setPasswordHash(passwordEncoder.encode("password"));
      adminUser.setActive(true);
      adminUser.setSuperuser(true);
      userAccountRepository.saveAndFlush(adminUser);

      // 2. Login as this admin
      MockHttpSession session =
          (MockHttpSession)
              mockMvc
                  .perform(
                      post("/api/v1/auth/login")
                          .contentType(MediaType.APPLICATION_JSON)
                          .content("{\"username\": \"test_admin_delete\", \"password\": \"password\"}"))
                  .andExpect(status().isOk())
                  .andReturn()
                  .getRequest()
                  .getSession(false);

      // 3. Try to delete themselves
      mockMvc
          .perform(
              delete("/api/v1/users/" + adminUser.getId())
                  .session(session))
          .andExpect(status().isBadRequest());
  }
}
