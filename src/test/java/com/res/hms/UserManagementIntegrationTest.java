package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.res.hms.domain.UserAccount;
import com.res.hms.user.repository.UserAccountRepository;
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
class UserManagementIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void adminCanResetUserPassword() throws Exception {
    // 1. Setup a user
    UserAccount staffUser = new UserAccount();
    staffUser.setUsername("test_staff");
    staffUser.setPasswordHash(passwordEncoder.encode("old_pass"));
    staffUser.setActive(true);
    userAccountRepository.saveAndFlush(staffUser);

    // 2. Login as admin
    MockHttpSession adminSession =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"admin\", \"password\": \"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);

    // 3. Reset user password as admin
    mockMvc
        .perform(
            post("/api/v1/users/" + staffUser.getId() + "/reset-password")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newPassword\": \"completely_new_pass\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    // 4. Try login as staff with old password - should fail
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"test_staff\", \"password\": \"old_pass\"}"))
        .andExpect(status().isUnauthorized());

    // 5. Try login as staff with new password - should succeed
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"test_staff\", \"password\": \"completely_new_pass\"}"))
        .andExpect(status().isOk());
  }
}
