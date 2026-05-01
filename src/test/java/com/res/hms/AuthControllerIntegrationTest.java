package com.res.hms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void loginCreatesHttpSessionAndAllowsProfileLookup() throws Exception {
    // 1. Login -> returns success with null data
    MockHttpSession session =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                                {
                                  "username": "admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn()
                .getRequest()
                .getSession(false);

    org.junit.jupiter.api.Assertions.assertNotNull(session);

    // 2. Must call /me to get profile
    mockMvc
        .perform(get("/api/v1/auth/me").session(session))
        .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.username").value("admin"))
        .andExpect(jsonPath("$.data.id").exists())
        .andExpect(jsonPath("$.data.superuser").value(true))
        .andExpect(jsonPath("$.data.memberships").isArray());
  }

  @Test
  void changePasswordShouldUpdateCredentials() throws Exception {
    // 1. Login with original password
    MockHttpSession session =
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

    // 2. Change password
    mockMvc
        .perform(
            post("/api/v1/auth/change-password")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\": \"admin123\", \"newPassword\": \"newAdminPass\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    // 3. Logout
    mockMvc.perform(post("/api/v1/auth/logout").session(session)).andExpect(status().isOk());

    // 4. Try login with old password - should fail
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"admin\", \"password\": \"admin123\"}"))
        .andExpect(status().isUnauthorized());

    // 5. Try login with new password - should succeed
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"admin\", \"password\": \"newAdminPass\"}"))
        .andExpect(status().isOk());

    // Cleanup: Change password back to original for other tests
    MockHttpSession cleanupSession =
        (MockHttpSession)
            mockMvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"admin\", \"password\": \"newAdminPass\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);

    mockMvc
        .perform(
            post("/api/v1/auth/change-password")
                .session(cleanupSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\": \"newAdminPass\", \"newPassword\": \"admin123\"}"))
        .andExpect(status().isOk());
  }
}
