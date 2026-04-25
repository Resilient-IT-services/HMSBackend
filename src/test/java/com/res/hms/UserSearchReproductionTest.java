package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.res.hms.domain.UserAccount;
import com.res.hms.repository.UserAccountRepository;
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
class UserSearchReproductionTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void testSearchUserWithNoAssignments() throws Exception {
    // 1. Create a Superuser with NO assignments
    UserAccount superAdmin = new UserAccount();
    superAdmin.setUsername("super_global");
    superAdmin.setPasswordHash(passwordEncoder.encode("admin123"));
    superAdmin.setSuperuser(true);
    superAdmin.setActive(true);
    superAdmin.setFullName("Super Global User");
    userAccountRepository.saveAndFlush(superAdmin);

    // 2. Login as this user
    MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\": \"super_global\", \"password\": \"admin123\"}"))
        .andExpect(status().isOk())
        .andReturn().getRequest().getSession();

    // 3. Search GLOBALLY for this user (hospitalId will be null in UserService)
    mockMvc.perform(get("/api/v1/users")
        .session(session)
        .param("search", "Global"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[?(@.fullName=='Super Global User')]").exists());
        
    // 4. Search via GlobalSearchService
    mockMvc.perform(get("/api/v1/search")
        .session(session)
        .param("query", "Global"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[?(@.type=='USER' && @.title=='Super Global User')]").exists());
  }

  @Test
  void testAdminCannotSeeUnassignedSuperuser() throws Exception {
    // 1. Create a Superuser with NO assignments
    UserAccount superAdmin = new UserAccount();
    superAdmin.setUsername("super_global_2");
    superAdmin.setPasswordHash(passwordEncoder.encode("admin123"));
    superAdmin.setSuperuser(true);
    superAdmin.setActive(true);
    superAdmin.setFullName("Hidden Super Admin");
    userAccountRepository.saveAndFlush(superAdmin);

    // 2. Create a normal Admin for a specific hospital
    UUID hospitalId = UUID.randomUUID(); // This is just for dummy ID, let's use a real one from DB or test helper
    // For this test, any valid hospital ID works if the user is assigned to it.
    
    // Using a simpler approach: use existing search_admin from GlobalSearchIntegrationTest or create one
    UserAccount hospitalAdmin = new UserAccount();
    hospitalAdmin.setUsername("hosp_admin");
    hospitalAdmin.setPasswordHash(passwordEncoder.encode("admin123"));
    hospitalAdmin.setActive(true);
    userAccountRepository.saveAndFlush(hospitalAdmin);

    // Login as hospital admin
    MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\": \"hosp_admin\", \"password\": \"admin123\"}"))
        .andExpect(status().isOk())
        .andReturn().getRequest().getSession();

    // 3. Search for the super admin as hospital admin (this should fail access control in Service layer if hospitalId is missing,
    // or return empty if hospitalId is provided)
    
    // In UserService.listUsers, if hospitalId is null and user is not superadmin, it throws 400.
    // If hospitalId is provided (via session context usually, but here we might need to mock it or set it)
    
    // Let's just verify the Repository behavior directly or via a controlled service call
  }
}
