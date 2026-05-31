package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.res.hms.iam.domain.RoleCode;
import com.res.hms.iam.domain.UserAccount;
import com.res.hms.iam.domain.UserContract;
import com.res.hms.iam.infrastructure.repository.RoleRepository;
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
class UserManagementIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestDataHelper testDataHelper;

  @Test
  void testUserListingWithFilteringAndSorting() throws Exception {
    UUID hospitalId = testDataHelper.createHospitalIfMissing("USER_TEST", "User Test Hospital");
    
    // 1. Setup multiple users
    createUser(hospitalId, "alice_user", "Alice Smith", RoleCode.NURSE, true);
    createUser(hospitalId, "bob_user", "Bob Jones", RoleCode.DOCTOR, true);
    createUser(hospitalId, "charlie_user", "Charlie Brown", RoleCode.NURSE, false);

    // 2. Login as admin
    MockHttpSession adminSession = (MockHttpSession) mockMvc
        .perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\": \"admin\", \"password\": \"admin123\"}"))
        .andExpect(status().isOk())
        .andReturn().getRequest().getSession();

    // 3. Filter by role=NURSE
    mockMvc.perform(get("/api/v1/users")
            .session(adminSession)
            .header("X-Hospital-Id", hospitalId.toString())
            .param("role", "NURSE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(2));

    // 4. Filter by active=false
    mockMvc.perform(get("/api/v1/users")
            .session(adminSession)
            .header("X-Hospital-Id", hospitalId.toString())
            .param("active", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].fullName").value("Charlie Brown"));

    // 5. Search by name
    mockMvc.perform(get("/api/v1/users")
            .session(adminSession)
            .header("X-Hospital-Id", hospitalId.toString())
            .param("search", "Jones"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].fullName").value("Bob Jones"));

    // 6. Sort by fullName desc
    mockMvc.perform(get("/api/v1/users")
            .session(adminSession)
            .header("X-Hospital-Id", hospitalId.toString())
            .param("sort", "fullName,desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].fullName").value("Charlie Brown"))
        .andExpect(jsonPath("$.data.content[1].fullName").value("Bob Jones"))
        .andExpect(jsonPath("$.data.content[2].fullName").value("Alice Smith"));
  }

  private void createUser(UUID hospitalId, String username, String fullName, RoleCode role, boolean active) {
    UserAccount user = new UserAccount();
    user.setUsername(username);
    user.setFullName(fullName);
    user.setPasswordHash(passwordEncoder.encode("password"));
    user.setActive(active);
    
    UserContract contract = new UserContract();
    contract.setUser(user);
    contract.setHospitalId(hospitalId);
    contract.setRole(roleRepository.findByCode(role).orElseThrow());
    contract.setActive(true);
    user.getContracts().add(contract);
    
    userAccountRepository.save(user);
  }

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
